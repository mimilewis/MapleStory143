/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.cashshop;

/**
 * @author ODINMR
 */
public class CashItemForSql {

    private final int category, subcategory, parent, sn, itemid, flag, price, discountPrice, quantity, expire, gender, likes;
    private final String image;

    public CashItemForSql(int category, int subcategory, int parent, String image, int sn, int itemid, int flag, int price, int discountPrice, int quantity, int expire, int gender, int likes) {
        this.category = category;
        this.subcategory = subcategory;
        this.parent = parent;
        this.image = image;
        this.sn = sn;
        this.itemid = itemid;
        this.flag = flag;
        this.price = price;
        this.discountPrice = discountPrice;
        this.quantity = quantity;
        this.expire = expire;
        this.gender = gender;
        this.likes = likes;
    }

    /**
     * 获取道具类别
     *
     * @return
     */
    public int getCategory() {
        return category;
    }

    /**
     * 获取道具子类别
     *
     * @return
     */
    public int getSubCategory() {
        return subcategory;
    }

    /**
     * 获取道具父系
     *
     * @return
     */
    public int getParent() {
        return parent;
    }

    /**
     * 获取道具图像
     *
     * @return
     */
    public String getImage() {
        return image;
    }

    /**
     * 获取道具唯一编号
     *
     * @return
     */
    public int getSN() {
        return sn;
    }

    /**
     * 获取道具ID
     *
     * @return
     */
    public int getItemId() {
        return itemid;
    }

    /**
     * 获取道具标签
     *
     * @return
     */
    public int getFlag() {
        return flag;
    }

    /**
     * 获取道具价格
     *
     * @return
     */
    public int getPrice() {
        return price;
    }

    /**
     * 获取道具折扣价格
     *
     * @return
     */
    public int getDiscountPrice() {
        return discountPrice;
    }

    /**
     * 获取道具数量
     *
     * @return
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * 获取道具过期时间
     *
     * @return
     */
    public int getExpire() {
        return expire;
    }

    /**
     * 获取道具性别限制
     *
     * @return
     */
    public int getGender() {
        return gender;
    }

    /**
     * 获取道具链接
     *
     * @return
     */
    public int getLikes() {
        return likes;
    }
}
