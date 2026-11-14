package com.yzz.product.controller;

import com.yzz.common.api.R;
import com.yzz.product.entity.PkgPackage;
import com.yzz.product.entity.ProdProduct;
import com.yzz.product.service.PackageService;
import com.yzz.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 官网、小程序、H5 读取在售套餐与商品（T56、T72）。
 *
 * <p>只返回上架数据，不返回成本与内部备注；租户由请求头 X-Tenant-Code 或子域名解析。</p>
 */
@Tag(name = "开放接口-套餐商品")
@RestController
@RequestMapping("/api/open/catalog")
public class OpenCatalogController {

    private final PackageService packageService;
    private final ProductService productService;

    public OpenCatalogController(PackageService packageService, ProductService productService) {
        this.packageService = packageService;
        this.productService = productService;
    }

    @Operation(summary = "在售套餐")
    @GetMapping("/packages")
    public R<List<PkgPackage>> packages(@RequestParam(required = false) Integer days,
                                        @RequestParam(required = false) String level) {
        List<PkgPackage> rows = packageService.list("ON_SHELF", null);
        List<PkgPackage> filtered = rows.stream()
                .filter(p -> days == null || days.equals(p.getDays()))
                .filter(p -> level == null || level.isBlank() || level.equals(p.getRoomTypeLevel()))
                .toList();
        return R.ok(filtered);
    }

    @Operation(summary = "套餐详情")
    @GetMapping("/packages/{id}")
    public R<Map<String, Object>> packageDetail(@PathVariable Long id) {
        PkgPackage pkg = packageService.detail(id);
        return R.ok(Map.of("package", pkg, "items", packageService.items(id)));
    }

    @Operation(summary = "在售商品")
    @GetMapping("/products")
    public R<List<ProdProduct>> products(@RequestParam(required = false) String category) {
        return R.ok(productService.list("ON_SHELF", category, null));
    }
}
