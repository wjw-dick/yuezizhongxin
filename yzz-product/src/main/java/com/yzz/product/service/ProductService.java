package com.yzz.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.sensitive.SensitiveWordFilter;
import com.yzz.common.tenant.TenantContext;
import com.yzz.product.entity.ProdProduct;
import com.yzz.product.entity.ProdSku;
import com.yzz.product.mapper.ProdProductMapper;
import com.yzz.product.mapper.ProdSkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品与规格（T33）。
 *
 * <p>库存扣减使用「条件更新」而非先查后写：`stock >= n` 时才更新，
 * 避免并发超卖；取消订单时按流水回补。</p>
 */
@Service
public class ProductService {

    private final ProdProductMapper productMapper;
    private final ProdSkuMapper skuMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    public ProductService(ProdProductMapper productMapper, ProdSkuMapper skuMapper,
                          SensitiveWordFilter sensitiveWordFilter) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    public List<ProdProduct> list(String status, String category, String keyword) {
        return productMapper.selectList(Wrappers.<ProdProduct>lambdaQuery()
                .eq(status != null && !status.isBlank(), ProdProduct::getStatus, status)
                .eq(category != null && !category.isBlank(), ProdProduct::getCategory, category)
                .like(keyword != null && !keyword.isBlank(), ProdProduct::getName, keyword)
                .orderByDesc(ProdProduct::getId));
    }

    public List<ProdSku> skus(Long productId) {
        return skuMapper.selectList(Wrappers.<ProdSku>lambdaQuery()
                .eq(ProdSku::getProductId, productId).orderByAsc(ProdSku::getId));
    }

    @Transactional
    public Long save(ProdProduct input) {
        if (input.getName() == null || input.getName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "商品名称不能为空");
        }
        sensitiveWordFilter.assertClean(input.getName() + " " + (input.getDetailHtml() == null ? "" : input.getDetailHtml()));
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getId() == null) {
            if (input.getStatus() == null) {
                input.setStatus("OFF_SHELF");
            }
            if (input.getStock() == null) {
                input.setStock(0);
            }
            productMapper.insert(input);
        } else {
            ProdProduct exist = productMapper.selectById(input.getId());
            if (exist == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "商品不存在");
            }
            input.setTenantId(exist.getTenantId());
            productMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        ProdProduct product = productMapper.selectById(id);
        if (product == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (!List.of("ON_SHELF", "OFF_SHELF").contains(status)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "非法状态：" + status);
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    @Transactional
    public void delete(Long id) {
        productMapper.deleteById(id);
        skuMapper.delete(Wrappers.<ProdSku>lambdaQuery().eq(ProdSku::getProductId, id));
    }

    @Transactional
    public void saveSkus(Long productId, List<ProdSku> skus) {
        ProdProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "商品不存在");
        }
        skuMapper.delete(Wrappers.<ProdSku>lambdaQuery().eq(ProdSku::getProductId, productId));
        for (ProdSku sku : skus) {
            ProdSku entity = new ProdSku();
            entity.setTenantId(TenantContext.requireTenantId());
            entity.setProductId(productId);
            entity.setSkuName(sku.getSkuName());
            entity.setPrice(sku.getPrice() == null ? product.getPrice() : sku.getPrice());
            entity.setStock(sku.getStock() == null ? 0 : sku.getStock());
            skuMapper.insert(entity);
        }
    }

    /**
     * 扣减库存。返回 false 表示库存不足。
     */
    @Transactional
    public boolean deduct(Long productId, Long skuId, int quantity) {
        if (skuId != null) {
            int updated = skuMapper.update(null, Wrappers.<ProdSku>lambdaUpdate()
                    .setSql("stock = stock - " + quantity)
                    .eq(ProdSku::getId, skuId)
                    .ge(ProdSku::getStock, quantity));
            return updated > 0;
        }
        int updated = productMapper.update(null, Wrappers.<ProdProduct>lambdaUpdate()
                .setSql("stock = stock - " + quantity)
                .eq(ProdProduct::getId, productId)
                .ge(ProdProduct::getStock, quantity));
        return updated > 0;
    }

    /** 取消订单或退货时回补库存。 */
    @Transactional
    public void restore(Long productId, Long skuId, int quantity) {
        if (skuId != null) {
            skuMapper.update(null, Wrappers.<ProdSku>lambdaUpdate()
                    .setSql("stock = stock + " + quantity).eq(ProdSku::getId, skuId));
        } else {
            productMapper.update(null, Wrappers.<ProdProduct>lambdaUpdate()
                    .setSql("stock = stock + " + quantity).eq(ProdProduct::getId, productId));
        }
    }
}
