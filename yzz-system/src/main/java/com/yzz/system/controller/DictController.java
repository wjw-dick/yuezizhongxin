package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.system.dto.DictCategoryRequest;
import com.yzz.system.dto.DictItemRequest;
import com.yzz.system.entity.SysDictCategory;
import com.yzz.system.entity.SysDictItem;
import com.yzz.system.service.DictService;
import com.yzz.system.vo.DictOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据字典维护（T29）。页面只渲染中文名称，编码仅在接口中传递。
 */
@Tag(name = "租户-数据字典")
@RestController
@RequestMapping("/api/tenant/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "字典分类列表")
    @GetMapping("/categories")
    public R<List<SysDictCategory>> categories() {
        AuthGuard.requireTenant();
        return R.ok(dictService.categories());
    }

    @Operation(summary = "新增字典分类")
    @AuditLog(module = "数据字典", action = "新增分类")
    @PostMapping("/categories")
    public R<Void> createCategory(@Valid @RequestBody DictCategoryRequest request) {
        AuthGuard.requireTenant();
        dictService.createCategory(request);
        return R.ok();
    }

    @Operation(summary = "修改字典分类")
    @AuditLog(module = "数据字典", action = "修改分类")
    @PutMapping("/categories/{id}")
    public R<Void> updateCategory(@PathVariable Long id,
                                  @Valid @RequestBody DictCategoryRequest request) {
        AuthGuard.requireTenant();
        dictService.updateCategory(id, request);
        return R.ok();
    }

    @Operation(summary = "删除字典分类")
    @AuditLog(module = "数据字典", action = "删除分类")
    @DeleteMapping("/categories/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        AuthGuard.requireTenant();
        dictService.deleteCategory(id);
        return R.ok();
    }

    @Operation(summary = "字典项列表")
    @GetMapping("/categories/{category}/items")
    public R<List<SysDictItem>> items(@PathVariable String category) {
        AuthGuard.requireTenant();
        return R.ok(dictService.items(category));
    }

    @Operation(summary = "新增字典项")
    @AuditLog(module = "数据字典", action = "新增字典项")
    @PostMapping("/categories/{category}/items")
    public R<Void> createItem(@PathVariable String category,
                              @Valid @RequestBody DictItemRequest request) {
        AuthGuard.requireTenant();
        dictService.createItem(category, request);
        return R.ok();
    }

    @Operation(summary = "修改字典项")
    @AuditLog(module = "数据字典", action = "修改字典项")
    @PutMapping("/items/{id}")
    public R<Void> updateItem(@PathVariable Long id, @Valid @RequestBody DictItemRequest request) {
        AuthGuard.requireTenant();
        dictService.updateItem(id, request);
        return R.ok();
    }

    @Operation(summary = "删除字典项")
    @AuditLog(module = "数据字典", action = "删除字典项")
    @DeleteMapping("/items/{id}")
    public R<Void> deleteItem(@PathVariable Long id) {
        AuthGuard.requireTenant();
        dictService.deleteItem(id);
        return R.ok();
    }

    @Operation(summary = "字典下拉选项（只含启用项）")
    @GetMapping("/options/{category}")
    public R<List<DictOptionVO>> options(@PathVariable String category) {
        AuthGuard.requireTenant();
        return R.ok(dictService.options(category));
    }
}
