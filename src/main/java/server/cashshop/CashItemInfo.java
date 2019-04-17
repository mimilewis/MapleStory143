package server.cashshop;

import org.jetbrains.annotations.NotNull;

public class CashItemInfo {

    private final int itemId; //道具ID
    private final int count; //默认购买数量
    private final int price; //道具价格
    private final int meso;
    private final int originalPrice; //道具原价
    private final int sn; //道具的SN
    private final int period; //道具的持续时间 也就是道具购买有有时间限制
    private final int gender; //道具是否有性别限制
    private final byte csClass;
    private final byte priority;
    private final int termStart;
    private final int termEnd;
    private final boolean onSale; //道具是否出售
    private final boolean bonus; //是否干什么的奖金
    private final boolean refundable; //是否可以回购换成抵用卷？
    private final boolean discount; //道具是否打折出售

    public CashItemInfo(int itemId, int count, int price, int originalPrice, int meso, int sn, int period, int gender, byte csClass, byte priority, int termStart, int termEnd, boolean sale, boolean bonus, boolean refundable, boolean discount) {
        this.itemId = itemId;
        this.count = count;
        this.price = price;
        this.originalPrice = originalPrice;
        this.meso = meso;
        this.sn = sn;
        this.period = period;
        this.gender = gender;
        this.csClass = csClass;
        this.priority = priority;
        this.termStart = termStart;
        this.termEnd = termEnd;
        this.onSale = sale;
        this.bonus = bonus;
        this.refundable = refundable;
        this.discount = discount;
    }

    /**
     * 获取道具ID
     *
     * @return
     */
    public int getItemId() {
        return itemId;
    }

    /**
     * 获取道具数量
     *
     * @return
     */
    public int getCount() {
        return count;
    }

    /*
     * 道具的价格
     * 暂时取最高价格
     */
    public int getPrice() {
        return Math.max(price, originalPrice);
    }

    /*
     * 道具原始价格
     */
    public int getOriginalPrice() {
        return originalPrice;
    }

    public int getSN() {
        return sn;
    }

    public int getPeriod() {
        return period;
    }

    public int getGender() {
        return gender;
    }

    public boolean onSale() {
        CashModInfo modInfo = CashItemFactory.getInstance().getModInfo(sn);
        if (modInfo != null) {
            return modInfo.showUp;
        }
        return onSale;
    }

    public boolean genderEquals(int g) {
        return g == this.gender || this.gender == 2;
    }

    public boolean isBonus() {
        return bonus;
    }

    public boolean isRefundable() {
        return refundable;
    }

    public boolean isDiscount() {
        return discount;
    }

    public int getMeso() {
        return meso;
    }

    public byte getCsClass() {
        return csClass;
    }

    public byte getPriority() {
        return priority;
    }

    public int getTermStart() {
        return termStart;
    }

    public int getTermEnd() {
        return termEnd;
    }

    public static class CashModInfo {

        private int discountPrice, mark, priority, sn, itemid, flags, period, gender, count, meso, csClass, termStart, termEnd, extra_flags, fameLimit, levelLimit, categories;
        private boolean showUp, packagez, base_new;
        private CashItemInfo cii;

        public CashModInfo(int sn, int discount, int mark, boolean show, int itemid, int priority, boolean packagez, int period, int gender, int count, int meso, int csClass, int termStart, int termEnd, int extra_flags, int fameLimit, int levelLimit, int categories, boolean base_new) {
            this.sn = sn;
            this.itemid = itemid;
            this.discountPrice = discount;
            this.mark = mark; //0 = new, 1 = sale, 2 = hot, 3 = event
            this.showUp = show;
            this.priority = priority;
            this.packagez = packagez;
            this.period = period;
            this.gender = gender;
            this.count = count;
            this.meso = meso;
            this.csClass = csClass; //0 = doesn't have, 1 = has, but false, 2 = has and true
            this.termStart = termStart;
            this.termEnd = termEnd;
            this.extra_flags = extra_flags;
            this.flags = extra_flags;
            this.fameLimit = fameLimit;
            this.levelLimit = levelLimit;
            this.categories = categories;
            this.base_new = base_new;

            if (this.itemid > 0) {
                this.flags |= 0x1;
            }
            if (this.count > 0) {
                this.flags |= 0x2;
            }
            if (this.discountPrice > 0) {
                this.flags |= 0x4;
            }
//            if (this.csClass > 0) {
//                this.flags |= 0x8;
//            }
            if (this.priority >= 0) {
                this.flags |= 0x10;
            }
            if (this.period > 0) {
                this.flags |= 0x20;
            }
            //0x40 = ?
            if (this.meso > 0) {
                this.flags |= 0x80;
            }
            if (this.gender >= 0) {
                this.flags |= 0x200;
            }
//            if (this.showUp) {
            this.flags |= 0x400;
//            }
            if (this.mark >= -1 || this.mark <= 0xF) {
                this.flags |= 0x800;
            }
            //0x2000, 0x4000, 0x8000, 0x10000, 0x20000, 0x100000, 0x80000 - ?
            if (this.fameLimit > 0) {
                this.flags |= 0x20000;
            }
            if (this.levelLimit > 0) {
                this.flags |= 0x40000;
            }
            if (this.termStart > 0) {
                this.flags |= 0x80000;
            }
            if (this.termEnd > 0) {
                this.flags |= 0x100000;
            }
            if (this.categories > 0) {
                this.flags |= 0x800000;
            }
        }

        public int getDiscountPrice() {
            return discountPrice;
        }

        public void setDiscountPrice(int discountPrice) {
            this.discountPrice = discountPrice;
        }

        public int getMark() {
            return mark;
        }

        public void setMark(int mark) {
            this.mark = mark;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public int getSn() {
            return sn;
        }

        public void setSn(int sn) {
            this.sn = sn;
        }

        public int getItemid() {
            return itemid;
        }

        public void setItemid(int itemid) {
            this.itemid = itemid;
        }

        public int getFlags() {
            return flags;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int period) {
            this.period = period;
        }

        public int getGender() {
            return gender;
        }

        public void setGender(int gender) {
            this.gender = gender;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getMeso() {
            return meso;
        }

        public void setMeso(int meso) {
            this.meso = meso;
        }

        public int getCsClass() {
            return csClass;
        }

        public void setCsClass(int csClass) {
            this.csClass = csClass;
        }

        public int getTermStart() {
            return termStart;
        }

        public void setTermStart(int termStart) {
            this.termStart = termStart;
        }

        public int getTermEnd() {
            return termEnd;
        }

        public void setTermEnd(int termEnd) {
            this.termEnd = termEnd;
        }

        public int getExtra_flags() {
            return extra_flags;
        }

        public void setExtra_flags(int extra_flags) {
            this.extra_flags = extra_flags;
        }

        public int getFameLimit() {
            return fameLimit;
        }

        public void setFameLimit(int fameLimit) {
            this.fameLimit = fameLimit;
        }

        public int getLevelLimit() {
            return levelLimit;
        }

        public void setLevelLimit(int levelLimit) {
            this.levelLimit = levelLimit;
        }

        public int getCategories() {
            return categories;
        }

        public void setCategories(int categories) {
            this.categories = categories;
        }

        public boolean isShowUp() {
            return showUp;
        }

        public void setShowUp(boolean showUp) {
            this.showUp = showUp;
        }

        public boolean isPackagez() {
            return packagez;
        }

        public void setPackagez(boolean packagez) {
            this.packagez = packagez;
        }

        public boolean isBase_new() {
            return base_new;
        }

        public void setBase_new(boolean base_new) {
            this.base_new = base_new;
        }

        public CashItemInfo getCii() {
            return cii;
        }

        public void setCii(CashItemInfo cii) {
            this.cii = cii;
        }

        public CashItemInfo toCItem(@NotNull CashItemInfo backup) {
            if (cii != null) {
                return cii;
            }
            final int item, c, price, expire, gen, likes;
            final boolean onSale;
            if (itemid <= 0) {
                item = backup.getItemId();
            } else {
                item = itemid;
            }
            if (count <= 0) {
                c = backup.getCount();
            } else {
                c = count;
            }
            if (meso <= 0) {
                if (discountPrice <= 0) {
                    price = backup.getPrice();
                } else {
                    price = discountPrice;
                }
            } else {
                price = meso;
            }
            if (period <= 0) {
                expire = backup.getPeriod();
            } else {
                expire = period;
            }
            if (gender < 0) {
                gen = backup.getGender();
            } else {
                gen = gender;
            }
            if (!showUp) {
                onSale = backup.onSale();
            } else {
                onSale = showUp;
            }

            cii = new CashItemInfo(item, c, price, price, meso, sn, expire, gen, backup.csClass, backup.priority, backup.termStart, backup.termEnd, onSale, false, false, false);
            return cii;
        }
    }
}
