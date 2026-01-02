package ma.projet.restcontroller.web.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryDto {
    private Long id; // lecture seule
    @NotBlank
    private String code;
    @NotBlank
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
