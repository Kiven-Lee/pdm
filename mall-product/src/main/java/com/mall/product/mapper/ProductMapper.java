package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣减库存（使用乐观锁防止超卖）
     * <p>
     * SQL 中加入 stock >= quantity 条件，确保库存充足时才扣减，
     * 返回影响行数：1=扣减成功，0=库存不足
     * </p>
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 影响行数
     */
    @Update("UPDATE product SET stock = stock - #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 回滚库存（订单取消时调用）
     *
     * @param productId 商品 ID
     * @param quantity  回滚数量
     */
    @Update("UPDATE product SET stock = stock + #{quantity} " +
            "WHERE id = #{productId} AND deleted = 0")
    int rollbackStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
