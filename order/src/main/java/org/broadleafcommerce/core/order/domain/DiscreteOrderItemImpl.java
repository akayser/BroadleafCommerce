/*
 * #%L
 * BroadleafCommerce Framework
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package org.broadleafcommerce.core.order.domain;

import org.broadleafcommerce.common.copy.CreateResponse;
import org.broadleafcommerce.common.copy.MultiTenantCopyContext;
import org.broadleafcommerce.common.currency.util.BroadleafCurrencyUtils;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.persistence.DefaultPostLoaderDao;
import org.broadleafcommerce.common.persistence.PostLoaderDao;
import org.broadleafcommerce.common.presentation.AdminPresentation;
import org.broadleafcommerce.common.presentation.AdminPresentationClass;
import org.broadleafcommerce.common.presentation.AdminPresentationToOneLookup;
import org.broadleafcommerce.common.presentation.client.SupportedFieldType;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import com.broadleafcommerce.order.common.domain.OrderProduct;
import com.broadleafcommerce.order.common.domain.OrderProductImpl;
import com.broadleafcommerce.order.common.domain.OrderSku;
import com.broadleafcommerce.order.common.domain.OrderSkuImpl;
import com.broadleafcommerce.order.common.domain.dto.OrderSkuDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "BLC_DISCRETE_ORDER_ITEM")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region="blOrderElements")
@AdminPresentationClass(friendlyName = "DiscreteOrderItemImpl_discreteOrderItem")
public class DiscreteOrderItemImpl extends OrderItemImpl implements DiscreteOrderItem {

    private static final long serialVersionUID = 1L;
    
    @Column(name="BASE_RETAIL_PRICE", precision=19, scale=5)
    @AdminPresentation(excluded = true, friendlyName = "DiscreteOrderItemImpl_Base_Retail_Price", order=2,
            group = "DiscreteOrderItemImpl_Pricing", fieldType=SupportedFieldType.MONEY)
    protected BigDecimal baseRetailPrice;
    
    @Column(name="BASE_SALE_PRICE", precision=19, scale=5)
    @AdminPresentation(excluded = true, friendlyName = "DiscreteOrderItemImpl_Base_Sale_Price", order=2,
            group = "DiscreteOrderItemImpl_Pricing", fieldType= SupportedFieldType.MONEY)
    protected BigDecimal baseSalePrice;
    
    @ManyToOne(targetEntity = OrderSkuImpl.class, optional=false)
    @JoinColumn(name = "SKU_ID", nullable = false)
    @Index(name="DISCRETE_SKU_INDEX", columnNames={"SKU_ID"})
    @AdminPresentation(friendlyName = "DiscreteOrderItemImpl_Sku", order=Presentation.FieldOrder.SKU,
            group = OrderItemImpl.Presentation.Group.Name.Catalog, groupOrder = OrderItemImpl.Presentation.Group.Order.Catalog)
    @AdminPresentationToOneLookup()
    protected OrderSku sku;

    @ManyToOne(targetEntity = OrderProductImpl.class)
    @JoinColumn(name = "PRODUCT_ID")
    @Index(name="DISCRETE_PRODUCT_INDEX", columnNames={"PRODUCT_ID"})
    @AdminPresentation(friendlyName = "DiscreteOrderItemImpl_Product", order=Presentation.FieldOrder.PRODUCT,
            group = OrderItemImpl.Presentation.Group.Name.Catalog, groupOrder = OrderItemImpl.Presentation.Group.Order.Catalog)
    @AdminPresentationToOneLookup()
    protected OrderProduct product;

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(name="BLC_ORDER_ITEM_ADD_ATTR", joinColumns=@JoinColumn(name="ORDER_ITEM_ID"))
    @BatchSize(size = 50)
    @Deprecated
    protected Map<String, String> additionalAttributes = new HashMap<String, String>();
    
    @OneToMany(mappedBy = "discreteOrderItem", targetEntity = DiscreteOrderItemFeePriceImpl.class, cascade = { CascadeType.ALL }, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "blOrderElements")
    protected List<DiscreteOrderItemFeePrice> discreteOrderItemFeePrices = new ArrayList<DiscreteOrderItemFeePrice>();
    
    @Column(name = "SKU_ACTIVE")
    protected boolean active;

    @Override
    public OrderSku getSku() {
        return sku;
    }


    @Override
    public void setSku(OrderSku sku) {
        this.sku = sku;
    }
    
    @Override
    public void setSku(OrderSkuDTO skuDTO) {
        setSku(skuDTO.getSku());
        if (skuDTO.hasRetailPrice()) {
            this.baseRetailPrice = skuDTO.getRetailPrice().getAmount();
        }
        if (skuDTO.hasSalePrice()) {
            this.baseSalePrice = skuDTO.getSalePrice().getAmount();
        }
        this.active = skuDTO.isActive();
        this.discountsAllowed = skuDTO.isDiscountable();
        this.itemTaxable = skuDTO.isTaxable();
        setName(skuDTO.getName());
    }

    @Override
    public Boolean isTaxable() {
        return itemTaxable;
    }

    @Override
    public OrderProduct getProduct() {
        return product;
    }

    @Override
    public void setProduct(OrderProduct product) {
        this.product = product;
    }

    @Override
    public void setOrder(Order order) {
        if (order != null) {
            throw new IllegalStateException("Cannot set an Order on a DiscreteOrderItem that is already associated with a BundleOrderItem");
        }
        this.order = order;
    }

    @Override
    public Order getOrder() {
        return order;
    }

    protected boolean updateSalePrice() {
// TODO microservices - deal with dynamic sku pricing in the order domain
//        if (isSalePriceOverride()) {
//            return false;
//        }
//
//        Money skuSalePrice = null;
//
//        DynamicSkuPrices priceData = getSku().getPriceData();
//        if (priceData != null) {
//            skuSalePrice = priceData.getPriceForQuantity(quantity);
//        }
//        if (skuSalePrice == null) {
//            skuSalePrice = getSku().getSalePrice();
//        }
//
//        boolean updated = false;
//        //use the sku prices - the retail and sale prices could be null
//        if (skuSalePrice != null && !skuSalePrice.getAmount().equals(salePrice)) {
//            baseSalePrice = skuSalePrice.getAmount();
//            salePrice = skuSalePrice.getAmount();
//            updated = true;
//        }
//        
//        // If there is no more sale price (because it got removed) then detect that case as well
//        if (skuSalePrice == null && salePrice != null) {
//            baseSalePrice = null;
//            salePrice = null;
//            updated = true;
//        }
//
//        // Adjust prices by adding in fees if they are attached.
//        if (getDiscreteOrderItemFeePrices() != null) {
//            for (DiscreteOrderItemFeePrice fee : getDiscreteOrderItemFeePrices()) {
//                Money returnPrice = convertToMoney(salePrice);
//                if (returnPrice != null) {
//                    salePrice = returnPrice.add(fee.getAmount()).getAmount();
//                }
//            }
//        }
//        return updated;
        return false;
    }

    protected boolean updateRetailPrice() {
// TODO microservices - deal with dynamic sku pricing in the order domain
//        if (isRetailPriceOverride()) {
//            return false;
//        }
//        Money skuRetailPrice = getSku().getRetailPrice();
//
//        boolean updated = false;
//        //use the sku prices - the retail and sale prices could be null
//        if (skuRetailPrice != null && !skuRetailPrice.getAmount().equals(retailPrice)) {
//            baseRetailPrice = skuRetailPrice.getAmount();
//            retailPrice = skuRetailPrice.getAmount();
//            updated = true;
//        }
//
//        // Adjust prices by adding in fees if they are attached.
//        if (getDiscreteOrderItemFeePrices() != null) {
//            for (DiscreteOrderItemFeePrice fee : getDiscreteOrderItemFeePrices()) {
//                Money returnPrice = convertToMoney(retailPrice);
//                retailPrice = returnPrice.add(fee.getAmount()).getAmount();
//            }
//        }
//        return updated;
        return false;
    }

    @Override
    public boolean updateSaleAndRetailPrices() {
        boolean salePriceUpdated = updateSalePrice();
        boolean retailPriceUpdated = updateRetailPrice();
        if (!isRetailPriceOverride() && !isSalePriceOverride()) {
            if (salePrice != null && salePrice.compareTo(retailPrice) <= 0) {
                price = salePrice;
            } else {
                price = retailPrice;
            }
        }
        return salePriceUpdated || retailPriceUpdated;
    }

    @Override
    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    @Override
    public void setAdditionalAttributes(Map<String, String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }

    @Override
    public Money getBaseRetailPrice() {
        return convertToMoney(baseRetailPrice);
    }

    @Override
    public void setBaseRetailPrice(Money baseRetailPrice) {
        this.baseRetailPrice = baseRetailPrice==null?null:baseRetailPrice.getAmount();
    }

    @Override
    public Money getBaseSalePrice() {
        return convertToMoney(baseSalePrice);
    }

    @Override
    public void setBaseSalePrice(Money baseSalePrice) {
        this.baseSalePrice = baseSalePrice==null?null:baseSalePrice.getAmount();
    }

    @Override
    public List<DiscreteOrderItemFeePrice> getDiscreteOrderItemFeePrices() {
        return discreteOrderItemFeePrices;
    }

    @Override
    public void setDiscreteOrderItemFeePrices(List<DiscreteOrderItemFeePrice> discreteOrderItemFeePrices) {
        this.discreteOrderItemFeePrices = discreteOrderItemFeePrices;
    }

    @Override
    protected Money convertToMoney(BigDecimal amount) {
        return amount == null ? null : BroadleafCurrencyUtils.getMoney(amount, getOrder().getCurrency());
    }
    
    @Override
    public OrderItem clone() {
        DiscreteOrderItem orderItem = (DiscreteOrderItem) super.clone();
        if (discreteOrderItemFeePrices != null) {
            for (DiscreteOrderItemFeePrice feePrice : discreteOrderItemFeePrices) {
                DiscreteOrderItemFeePrice cloneFeePrice = feePrice.clone();
                cloneFeePrice.setDiscreteOrderItem(orderItem);
                orderItem.getDiscreteOrderItemFeePrices().add(cloneFeePrice);
            }
        }
        if (additionalAttributes != null) {
            orderItem.getAdditionalAttributes().putAll(additionalAttributes);
        }
        orderItem.setBaseRetailPrice(convertToMoney(baseRetailPrice));
        orderItem.setBaseSalePrice(convertToMoney(baseSalePrice));
        orderItem.setProduct(product);
        orderItem.setSku(sku);

        if (orderItem.getOrder() == null) {
            throw new IllegalStateException("Either an Order or a BundleOrderItem must be set on the DiscreteOrderItem");
        }
        
        return orderItem;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        DiscreteOrderItemImpl other = (DiscreteOrderItemImpl) obj;
        
        if (!super.equals(obj)) {
            return false;
        }

        if (id != null && other.id != null) {
            return id.equals(other.id);
        }

        if (sku == null) {
            if (other.sku != null) {
                return false;
            }
        } else if (!sku.equals(other.sku)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = super.hashCode();
        int result = 1;
        result = prime * result + ((sku == null) ? 0 : sku.hashCode());
        return result;
    }

    @Override
    public boolean isDiscountingAllowed() {
        if (discountsAllowed == null) {
            return false;
        }
        return discountsAllowed.booleanValue();
    }

    @Override
    public CreateResponse<DiscreteOrderItem> createOrRetrieveCopyInstance(MultiTenantCopyContext context) throws CloneNotSupportedException {
        CreateResponse<DiscreteOrderItem> createResponse = super.createOrRetrieveCopyInstance(context);
        if (createResponse.isAlreadyPopulated()) {
            return createResponse;
        }
        DiscreteOrderItem cloned = createResponse.getClone();
        cloned.setBaseRetailPrice(getBaseRetailPrice());
        cloned.setBaseSalePrice(getBaseSalePrice());
        cloned.setProduct(product);
        cloned.setSku(sku);
        cloned.setCategory(category);
        ((DiscreteOrderItemImpl)cloned).discountsAllowed = discountsAllowed;
        cloned.setName(name);
        // dont clone
        cloned.setOrder(order);
        return  createResponse;
    }

    public static class Presentation {
        public static class Tab {
            public static class Name {
                public static final String OrderItems = "OrderImpl_Order_Items_Tab";
            }

            public static class Order {
                public static final int OrderItems = 2000;
            }
        }

        public static class Group {
            public static class Name {
            }

            public static class Order {
            }
        }

        public static class FieldOrder {
            public static final int PRODUCT = 2000;
            public static final int SKU = 3000;
        }
    }

    @Override
    public boolean isSkuActive() {
        return active;
    }
}
