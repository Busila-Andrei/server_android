package com.example.server_android;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryDTO {
    private Long id;
    private String name;
    private Long categoryId;
}
