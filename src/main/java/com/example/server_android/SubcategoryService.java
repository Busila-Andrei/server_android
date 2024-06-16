package com.example.server_android;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;

    public List<SubcategoryDTO> getSubcategoriesByCategoryId(Long categoryId) {
        List<Subcategory> subcategories = subcategoryRepository.findByCategoryId(categoryId);
        return subcategories.stream()
                .map(subcategory -> new SubcategoryDTO(subcategory.getId(), subcategory.getName(), subcategory.getCategory().getId()))
                .collect(Collectors.toList());
    }
}
