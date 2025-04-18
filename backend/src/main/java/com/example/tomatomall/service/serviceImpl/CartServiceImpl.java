package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.CartItemRepository;
import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dao.StockpileRepository;
import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.vo.CartItemVO;
import com.example.tomatomall.vo.CartVO;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

@Service
public class CartServiceImpl implements CartService {

    @Resource
    private CartItemRepository cartItemRepository;

    @Resource
    private ProductRepository productRepository;

    @Resource
    private StockpileRepository stockpileRepository;

    @Override
    public CartItem addCartItem(Integer userId, Integer productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 通过 stockpileRepository 获取商品库存
        Stockpile stockpile = stockpileRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("商品库存信息不存在"));

        if (stockpile.getAmount() < quantity) {
            throw new RuntimeException("库存不足");
        }

        // 检查用户是否已将该商品添加到购物车
        List<CartItem> existingItems = cartItemRepository.findByUserIdAndProductId(userId, productId);
        CartItem cartItem;

        if (!existingItems.isEmpty()) {
            // 如果已存在，更新数量
            cartItem = existingItems.get(0);
            int newQuantity = cartItem.getQuantity() + quantity;
            if (stockpile.getAmount() < newQuantity) {
                throw new RuntimeException("库存不足，无法添加更多数量");
            }
            cartItem.setQuantity(newQuantity);
        } else {
            // 如果不存在，创建新购物车项
            cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
        }

        return cartItemRepository.save(cartItem);
    }

    @Override
    public void removeCartItem(Integer cartItemId) {
        if (!cartItemRepository.existsById(cartItemId)) {
            throw new RuntimeException("购物车商品不存在");
        }
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    public void updateCartItemQuantity(Integer cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("购物车商品不存在"));

        // 通过 stockpileRepository 获取商品库存
        Stockpile stockpile = stockpileRepository.findByProductId(cartItem.getProductId())
                .orElseThrow(() -> new RuntimeException("商品库存信息不存在"));

        if (stockpile.getAmount() < quantity) {
            throw new RuntimeException("库存不足");
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Override
    public CartVO getCartItems(Integer userId) {
        // 根据用户ID获取购物车项列表
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            // 购物车为空的情况
            CartVO emptyCart = new CartVO(new ArrayList<>(), 0, BigDecimal.ZERO);
            return emptyCart;
        }

        List<CartItemVO> cartItemVOs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 遍历购物车项，获取商品详情并计算总金额
        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + item.getProductId()));

            CartItemVO cartItemVO = new CartItemVO();
            cartItemVO.setCartItemId(item.getCartItemId());
            cartItemVO.setProductId(product.getId());
            cartItemVO.setTitle(product.getTitle());
            cartItemVO.setPrice(product.getPrice());
            cartItemVO.setDescription(product.getDescription());
            cartItemVO.setCover(product.getCover());
            cartItemVO.setDetail(product.getDetail());
            cartItemVO.setQuantity(item.getQuantity());

            cartItemVOs.add(cartItemVO);

            // 计算当前项的小计并添加到总金额
            BigDecimal itemPrice = product.getPrice();
            BigDecimal quantity = new BigDecimal(item.getQuantity());
            BigDecimal itemTotal = itemPrice.multiply(quantity);
            totalAmount = totalAmount.add(itemTotal);
        }

        // 创建并返回购物车VO对象
        CartVO cartVO = new CartVO(cartItemVOs, cartItemVOs.size(), totalAmount);
        return cartVO;
    }
}