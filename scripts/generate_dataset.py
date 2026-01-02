#!/usr/bin/env python3
"""
Generate dataset files for the REST benchmark:
- 2,000 categories (default)
- 100,000 items (default), ~50/category on average
- JSONL payloads for POST/PUT bodies:
  * small (~1 KB): fields sku, name, price, stock, categoryId
  * large (~5 KB): same + description text to reach target size
- CSVs for JMeter (ids and basic attributes)
- Optional SQL seed file with INSERT batches

Usage examples:
  python generate_dataset.py --out-dir ../../data
  python generate_dataset.py -c 2000 -i 100000 --out-dir ../../data --with-sql

The generator streams to files to keep memory usage low.
"""

from __future__ import annotations
import argparse
import csv
import json
import math
import os
import random
import string
from dataclasses import dataclass
from pathlib import Path
from typing import Iterator, Optional

DEFAULT_CATEGORIES = 2000
DEFAULT_ITEMS = 100_000
SMALL_TARGET = 1024        # ~1 KB
LARGE_TARGET = 5 * 1024    # ~5 KB

# -------- Helpers --------

def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def rand_price(rng: random.Random) -> float:
    # 1.00 .. 9999.99 with two decimals
    cents = rng.randint(100, 999999)
    return round(cents / 100.0, 2)


def rand_stock(rng: random.Random) -> int:
    # 0..500, slightly skewed toward lower values
    base = rng.random()
    return int((base ** 1.5) * 500)


_LOREM = (
    "lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua "
    "ut enim ad minim veniam quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat duis aute irure "
    "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur excepteur sint occaecat cupidatat non "
    "proident sunt in culpa qui officia deserunt mollit anim id est laborum "
)


def repeat_text_to_bytes(prefix: str, target_bytes: int) -> str:
    """Produce ASCII text such that len(text.encode('utf-8')) ~= target_bytes.
    We'll generate at least prefix + 1 char. The result is deterministic by prefix length.
    """
    if target_bytes <= 0:
        return prefix
    base = prefix.strip() + " " if prefix else ""
    # Repeat lorem to exceed target and then slice to exact byte length
    s = (base + _LOREM) * ((target_bytes // max(1, len(_LOREM))) + 3)
    b = s.encode("utf-8")
    if len(b) >= target_bytes:
        return b[:target_bytes].decode("utf-8", errors="ignore")
    # Fallback pad with 'x'
    pad = b + b"x" * (target_bytes - len(b))
    return pad.decode("utf-8", errors="ignore")


# -------- Data models --------

@dataclass
class CategoryRow:
    id: int
    code: str
    name: str


@dataclass
class ItemRow:
    id: int
    sku: str
    name: str
    price: float
    stock: int
    category_id: int
    description: Optional[str] = None


def gen_categories(count: int) -> Iterator[CategoryRow]:
    for i in range(1, count + 1):
        code = f"CAT{i:04d}"
        yield CategoryRow(id=i, code=code, name=f"Category {i:04d}")


def gen_items(count: int, categories: int, rng: random.Random, small_target: int, large_target: int) -> Iterator[ItemRow]:
    for i in range(1, count + 1):
        sku = f"SKU{i:06d}"
        # Distribute items across categories roughly uniformly, but with small jitter
        # Mapping 1..count -> 1..categories cycling ensures coverage
        base_cat = ((i - 1) % categories) + 1
        jitter = rng.randint(-5, 5)
        category_id = max(1, min(categories, base_cat + jitter))

        # Name sized to contribute to small payload size budget (no description case)
        # We'll keep a readable prefix and pad to ~ a few hundred bytes; remaining size depends on JSON overhead
        name_prefix = f"Item {i:06d}"
        # Base name around 64..256 bytes to keep reasonable; exact target tuning done in payload generation
        name = name_prefix

        price = rand_price(rng)
        stock = rand_stock(rng)

        # For CSV we don't need to hit payload sizes; we'll leave description empty here
        yield ItemRow(id=i, sku=sku, name=name, price=price, stock=stock, category_id=category_id)


# -------- Writers --------

def write_categories_csv(path: Path, rows: Iterator[CategoryRow]) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "code", "name"])  # header
        for r in rows:
            w.writerow([r.id, r.code, r.name])


def write_items_csv(path: Path, rows: Iterator[ItemRow]) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "sku", "name", "price", "stock", "category_id", "description"])  # header
        for r in rows:
            w.writerow([r.id, r.sku, r.name, f"{r.price:.2f}", r.stock, r.category_id, r.description or ""]) 


def write_ids_csv(path: Path, ids: Iterator[int]) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id"])  # header
        for i in ids:
            w.writerow([i])


# JSON payload sizing utilities

def json_size_bytes(obj: dict) -> int:
    return len(json.dumps(obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))


def fit_small_payload_bytes(base: dict, target_bytes: int) -> dict:
    # Adjust name length to approximate target size
    # 1) try with current name
    cur = dict(base)
    size = json_size_bytes(cur)
    if size >= target_bytes:
        return cur
    # 2) pad name
    deficit = target_bytes - size
    pad_text = repeat_text_to_bytes("", deficit)
    cur["name"] = (cur.get("name") or "") + pad_text
    return cur


def fit_large_payload_bytes(base: dict, target_bytes: int) -> dict:
    cur = dict(base)
    # Add description sized to reach target
    # Compute overhead with empty description first
    cur["description"] = ""
    base_size = json_size_bytes(cur)
    deficit = max(0, target_bytes - base_size)
    # Keep a short prefix for realism, then pad
    desc_prefix = "Autogenerated description. "
    padded = repeat_text_to_bytes(desc_prefix, deficit)
    cur["description"] = padded
    return cur


def write_payloads_jsonl(
    small_path: Path,
    large_path: Path,
    items_iter: Iterator[ItemRow],
    small_target: int,
    large_target: int,
) -> None:
    with small_path.open("w", encoding="utf-8") as fs, large_path.open("w", encoding="utf-8") as fl:
        for r in items_iter:
            base = {
                "sku": r.sku,
                "name": r.name,
                "price": round(r.price, 2),
                "stock": int(r.stock),
                "categoryId": int(r.category_id),
            }
            small_obj = fit_small_payload_bytes(base, small_target)
            large_obj = fit_large_payload_bytes(base, large_target)
            fs.write(json.dumps(small_obj, ensure_ascii=False) + "\n")
            fl.write(json.dumps(large_obj, ensure_ascii=False) + "\n")


def write_sql_seed(path: Path, cats: int, items: int) -> None:
    """Write a simple SQL seed file with INSERT batches.
    Note: COPY is faster, but INSERTs are more portable for a quick start.
    """
    with path.open("w", encoding="utf-8") as f:
        f.write("-- SQL seed generated by generate_dataset.py\n")
        f.write("-- Category\n")
        f.write("BEGIN;\n")
        # Categories
        for i in range(1, cats + 1):
            code = f"CAT{i:04d}"
            name = f"Category {i:04d}"
            safe_name = name.replace("'", "''")
            f.write(f"INSERT INTO category (id, code, name, updated_at) VALUES ({i}, '{code}', '{safe_name}', NOW());\n")
        # Items (minimal columns; description omitted for speed)
        f.write("-- Item\n")
        for i in range(1, items + 1):
            sku = f"SKU{i:06d}"
            # Cycle categories
            cat = ((i - 1) % cats) + 1
            price = (i % 999999) / 100.0 + 1.0
            stock = i % 501
            name = f"Item {i:06d}"
            safe_name = name.replace("'", "''")
            f.write(
                f"INSERT INTO item (id, sku, name, price, stock, category_id, updated_at) VALUES ({i}, '{sku}', '{safe_name}', {price:.2f}, {stock}, {cat}, NOW());\n"
            )
        f.write("COMMIT;\n")


# -------- CLI --------

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Generate dataset for REST benchmark")
    p.add_argument("--out-dir", type=Path, default=Path("../../data"), help="Output directory for generated files")
    p.add_argument("-c", "--categories", type=int, default=DEFAULT_CATEGORIES, help="Number of categories (default 2000)")
    p.add_argument("-i", "--items", type=int, default=DEFAULT_ITEMS, help="Number of items (default 100000)")
    p.add_argument("--seed", type=int, default=42, help="Random seed for reproducibility")
    p.add_argument("--small-bytes", type=int, default=SMALL_TARGET, help="Target size for small JSON payload (~1KB)")
    p.add_argument("--large-bytes", type=int, default=LARGE_TARGET, help="Target size for large JSON payload (~5KB)")
    p.add_argument("--with-sql", action="store_true", help="Also generate a SQL seed file with INSERTs")
    return p.parse_args()


def main() -> None:
    args = parse_args()
    rng = random.Random(args.seed)

    out_dir: Path = args.out_dir.resolve()
    ensure_dir(out_dir)

    # File paths
    categories_csv = out_dir / "categories.csv"
    items_csv = out_dir / "items.csv"
    category_ids_csv = out_dir / "category_ids.csv"
    item_ids_csv = out_dir / "item_ids.csv"
    items_small_jsonl = out_dir / "items_payload_small.jsonl"
    items_large_jsonl = out_dir / "items_payload_large.jsonl"
    seed_sql = out_dir / "seed.sql"

    # Generate and write categories
    cats_iter1 = list(gen_categories(args.categories))  # small enough (2000) to keep in memory for reuse
    cats_iter2 = (c for c in cats_iter1)
    write_categories_csv(categories_csv, iter(cats_iter1))
    write_ids_csv(category_ids_csv, (c.id for c in cats_iter2))

    # Items: we need three passes (CSV, JSON small/large, IDs) but avoid storing 100k in memory.
    # We'll regenerate deterministically based on index and seed.
    items_iter_for_csv = gen_items(args.items, args.categories, rng=random.Random(args.seed + 1), small_target=args.small_bytes, large_target=args.large_bytes)
    write_items_csv(items_csv, items_iter_for_csv)

    items_iter_for_json = gen_items(args.items, args.categories, rng=random.Random(args.seed + 1), small_target=args.small_bytes, large_target=args.large_bytes)
    write_payloads_jsonl(items_small_jsonl, items_large_jsonl, items_iter_for_json, args.small_bytes, args.large_bytes)

    write_ids_csv(item_ids_csv, (i for i in range(1, args.items + 1)))

    if args.with_sql:
        write_sql_seed(seed_sql, cats=args.categories, items=args.items)

    # Summary
    print("Generated files in:", out_dir)
    print("-", categories_csv.name)
    print("-", items_csv.name)
    print("-", category_ids_csv.name)
    print("-", item_ids_csv.name)
    print("-", items_small_jsonl.name)
    print("-", items_large_jsonl.name)
    if args.with_sql:
        print("-", seed_sql.name)


if __name__ == "__main__":
    main()
