package com.mall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.Result;
import com.mall.product.entity.Category;
import com.mall.product.entity.Product;
import com.mall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 分页查询商品列表
     * GET /product/list?page=1&size=10&categoryId=1&keyword=手机
     */
    @GetMapping("/list")
    public Result<Page<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.success(productService.listProducts(page, size, categoryId, keyword));
    }

    /**
     * 获取商品详情
     * GET /product/detail/{id}
     * X-User-Id 由网关注入（未登录时不存在）
     */
    @GetMapping("/detail/{id}")
    public Result<Product> detail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(productService.getProductDetail(id, userId));
    }

    /**
     * 获取分类列表
     * GET /product/category/list
     */
    @GetMapping("/category/list")
    public Result<List<Category>> categoryList() {
        return Result.success(productService.listCategories());
    }

    /**
     * 新增商品（管理端）
     * POST /product/add
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Product product) {
        productService.addProduct(product);
        return Result.success();
    }

    /**
     * 更新商品（管理端）
     * PUT /product/update
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Product product) {
        productService.updateProduct(product);
        return Result.success();
    }
}
