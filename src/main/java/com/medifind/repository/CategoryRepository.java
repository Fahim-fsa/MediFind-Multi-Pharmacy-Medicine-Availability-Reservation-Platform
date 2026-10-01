package com.medifind.repository;

import com.medifind.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * SLP: Core Platform & Shared Engine → "Implement shared Medicine
 * Catalogue & Category service"
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findAllByOrderByNameAsc();
}
