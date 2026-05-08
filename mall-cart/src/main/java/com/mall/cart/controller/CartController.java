package com.mall.cart.controller;

import com.mall.cart.dto.CartItem;
import com.mall.cart.service.CartService;
import com.mall.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车控制器
 * <p>
 * 所有接口需要登录，用户 ID 由网关注入到请求头 X-User-Id。
 * </p>
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 获取购物车列表
     * GET /cart/list
     */
    @GetMapping("/list")
    public Result<List<CartItem>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.getCartList(userId));
    }

    /**
     * 加入购物车
     * POST /cart/add
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestHeader("X-User-Id") Long userId,
                            @Valid @RequestBody AddCartRequest request) {
        cartService.addToCart(userId, request.getProductId(), request.getProductName(),
                request.getMainImage(), request.getPrice(), request.getQuantity());
        return Result.success();
    }

    /**
     * 修改购物车商品数量
     * PUT /cart/quantity
     */
    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam Long productId,
                                       @RequestParam @Min(1) Integer quantity) {
        cartService.updateQuantity(userId, productId, quantity);
        return Result.success();
    }

    /**
     * 删除购物车商品
     * DELETE /cart/remove/{productId}
     */
    @DeleteMapping("/remove/{productId}")
    public Result<Void> remove(@RequestHeader("X-User-Id") Long userId,
                               @PathVariable Long productId) {
        cartService.removeFromCart(userId, productId);
        return Result.success();
    }

    /**
     * 清空购物车
     * DELETE /cart/clear
     */
    @DeleteMapping("/clear")
    public Result<Void> clear(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return Result.success();
    }

    /**
     * 更新商品选中状态
     * PUT /cart/checked
     */
    @PutMapping("/checked")
    public Result<Void> updateChecked(@RequestHeader("X-User-Id") Long userId,
                                      @RequestParam Long productId,
                                      @RequestParam Boolean checked) {
        cartService.updateChecked(userId, productId, checked);
        return Result.success();
    }

    // ===== 内部请求体 DTO =====

    @Data
    public static class AddCartRequest {
        @NotNull(message = "商品ID不能为空")
        private Long productId;
        private String productName;
        private String mainImage;
        private BigDecimal price;
        @NotNull @Min(1)
        private Integer quantity;
    }
}
