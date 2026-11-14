package com.yzz.product.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.product.entity.PkgItem;
import com.yzz.product.entity.PkgPackage;
import com.yzz.product.entity.ProdProduct;
import com.yzz.product.entity.ProdSku;
import com.yzz.product.service.PackageService;
import com.yzz.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 套餐与商品（T33）。
 */
@Tag(name = "租户-套餐与商品")
@RestController
@RequestMapping("/api/tenant/catalog")
public class CatalogController {

    private final PackageService packageService;
    private final ProductService productService;

    public CatalogController(PackageService packageService, ProductService productService) {
        this.packageService = packageService;
        this.productService = productService;
    }

    @Operation(summary = "套餐列表")
    @GetMapping("/packages")
    public R<List<PkgPackage>> packages(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword) {
        AuthGuard.requireTenant();
        return R.ok(packageService.list(status, keyword));
    }

    @Operation(summary = "套餐详情")
    @GetMapping("/packages/{id}")
    public R<Map<String, Object>> packageDetail(@PathVariable Long id) {
        AuthGuard.requireTenant();
        PkgPackage pkg = packageService.detail(id);
        return R.ok(Map.of("package", pkg, "items", packageService.items(id),
                "deposit", packageService.depositOf(pkg)));
    }

    @Operation(summary = "保存套餐")
    @PostMapping("/packages")
    public R<Long> savePackage(@RequestBody PkgPackage request) {
        AuthGuard.requireTenant();
        return R.ok(packageService.save(request));
    }

    @Operation(summary = "套餐上下架")
    @PutMapping("/packages/{id}/status")
    public R<Void> packageStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        packageService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "删除套餐")
    @DeleteMapping("/packages/{id}")
    public R<Void> deletePackage(@PathVariable Long id) {
        AuthGuard.requireTenant();
        packageService.delete(id);
        return R.ok();
    }

    @Operation(summary = "保存套餐包含项")
    @PutMapping("/packages/{id}/items")
    public R<Void> saveItems(@PathVariable Long id, @RequestBody List<PkgItem> items) {
        AuthGuard.requireTenant();
        packageService.saveItems(id, items);
        return R.ok();
    }

    @Operation(summary = "商品列表")
    @GetMapping("/products")
    public R<List<ProdProduct>> products(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String keyword) {
        AuthGuard.requireTenant();
        return R.ok(productService.list(status, category, keyword));
    }

    @Operation(summary = "保存商品")
    @PostMapping("/products")
    public R<Long> saveProduct(@RequestBody ProdProduct request) {
        AuthGuard.requireTenant();
        return R.ok(productService.save(request));
    }

    @Operation(summary = "商品上下架")
    @PutMapping("/products/{id}/status")
    public R<Void> productStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        productService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/products/{id}")
    public R<Void> deleteProduct(@PathVariable Long id) {
        AuthGuard.requireTenant();
        productService.delete(id);
        return R.ok();
    }

    @Operation(summary = "商品规格列表")
    @GetMapping("/products/{id}/skus")
    public R<List<ProdSku>> skus(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(productService.skus(id));
    }

    @Operation(summary = "保存商品规格")
    @PutMapping("/products/{id}/skus")
    public R<Void> saveSkus(@PathVariable Long id, @RequestBody List<ProdSku> skus) {
        AuthGuard.requireTenant();
        productService.saveSkus(id, skus);
        return R.ok();
    }
}
