package server.console.groups.datamanage

import com.alee.extended.breadcrumb.WebBreadcrumb
import com.alee.extended.breadcrumb.WebBreadcrumbToggleButton
import com.alee.extended.layout.TableLayout
import com.alee.extended.panel.CenterPanel
import com.alee.extended.panel.GroupPanel
import com.alee.extended.panel.GroupingType
import com.alee.laf.button.WebButton
import com.alee.laf.combobox.WebComboBox
import com.alee.laf.label.WebLabel
import com.alee.laf.list.WebList
import com.alee.laf.optionpane.WebOptionPane
import com.alee.laf.panel.WebPanel
import com.alee.laf.progressbar.WebProgressBar
import com.alee.laf.radiobutton.WebRadioButton
import com.alee.laf.rootpane.WebDialog
import com.alee.laf.rootpane.WebFrame
import com.alee.laf.scroll.WebScrollPane
import com.alee.laf.tabbedpane.WebTabbedPane
import com.alee.laf.text.WebTextField
import com.alee.managers.tooltip.TooltipManager
import com.alee.utils.SwingUtils
import database.DatabaseConnection
import org.apache.logging.log4j.LogManager
import server.MapleItemInformationProvider
import server.cashshop.CashItemFactory
import server.cashshop.CashItemInfo
import server.cashshop.CashShopType
import tools.StringUtil
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.ListSelectionModel
import javax.swing.SwingWorker
import kotlin.Comparator

class CashShopPane(owner: WebFrame?) : TabbedPane(owner) {

    val log = LogManager.getLogger()!!

    companion object {
        val values = LinkedHashMap<String, Any>()
    }

    val panels = LinkedHashMap<Int, WebTabbedPane>()
    val centerPanel = object : WebPanel() {
        init {
            setMargin(5, 0, 5, 5)
        }
    }
    val typelist = object : WebList(CashShopType.values()) {
        init {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            fixedCellHeight = 50
            fixedCellWidth = 50
            fontSize = 20
        }
    }

    override fun init() {
        values.put("物品ID", 0)
        values.put("物品数量", 0)
        values.put("产品类型", arrayOf("新品", "折扣", "人气", "活动", "限量"))
        values.put("价格", 0)
//        values.put("折扣价格", 0)
//        values.put("分类", 0)
        values.put("开始销售时间", 0)
        values.put("停止销售时间", 0)
        values.put("性别", arrayOf("男", "女", "全部"))
        values.put("等级", 0)
        values.put("人气", 0)

        panels.put(CashShopType.搜索结果.ordinal, object : WebTabbedPane() {
            init {
                add(CashShopType.搜索结果.name, CashShopItemPane((CashShopType.搜索结果.ordinal + 10) * 100))
            }
        })
    }

    override fun getTitle() = "游戏商城"

    override fun isSelectedSearchResult(): Boolean {
        return true
    }

    override fun isSelectedSearchFuzzy(): Boolean {
        return false
    }

    override fun getLeftComponent(): Component {
        return object : WebPanel() {
            init {
                setMargin(5, 5, 5, 0)
                preferredWidth = 200

                typelist.addListSelectionListener { e ->
                    if (e.valueIsAdjusting && typelist.isEnabled) {
                        if (!panels.containsKey(typelist.selectedIndex)) {
                            showLoadPanel()
                        }
                        object : SwingWorker<Any, Any>() {
                            var newPanel: WebTabbedPane? = null
                            override fun doInBackground(): Any? {
                                newPanel = panels.computeIfAbsent(typelist.selectedIndex, {
                                    val source: WebList = e.source as WebList
                                    val tabpanel = WebTabbedPane()
                                    var subtype = CashShopType.valueOf(source.selectedValue.toString()).subtype
                                    if (subtype.isEmpty())
                                        subtype = arrayOf(source.selectedValue.toString())

                                    subtype.forEach { it ->
                                        try {
                                            val shopType = CashShopType.valueOf(source.selectedValue.toString())
                                            tabpanel.add(it, CashShopItemPane((shopType.ordinal + 10) * 100 + shopType.subtype.indexOf(it)))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    tabpanel
                                })
                                return null
                            }

                            override fun done() {
                                if (newPanel != null) {
                                    updateCenterPanel(newPanel!!)
                                }
                                typelist.isEnabled = true
                            }
                        }.execute()
                    }
                }

                add(GroupPanel(GroupingType.fillLast, false, searchGroup, WebScrollPane(typelist)), BorderLayout.WEST)
            }
        }
    }

    override fun doSearchAction(result: String) {
        if (result.isEmpty()) {
            return
        }
        showLoadPanel()
        typelist.selectedIndex = typelist.modelSize - 1

        object : SwingWorker<Any, Any>() {
            var searchPanel: WebTabbedPane? = null
            override fun doInBackground(): Any? {
                searchPanel = panels.entries.first().value
                searchPanel?.components?.forEach { it ->
                    if (it is CashShopItemPane) {
                        it.updateAllItems(CashItemFactory.getInstance().allItem.filter {
                            if (it.key < 100000000) {
                                return@filter false
                            } else if (StringUtil.isNumber(result)) {
                                return@filter it.key == result.toInt() || it.value.itemId == result.toInt()
                            } else {
                                return@filter MapleItemInformationProvider.getInstance().getName(it.value.itemId) == result
                            }
                        })
                        it.spawnPange(0)
                    }
                }
                return null
            }

            override fun done() {
                updateCenterPanel(searchPanel!!)
                typelist.isEnabled = true
            }
        }.execute()
    }

    fun showLoadPanel() {
        typelist.isEnabled = false
        val loadprogress = WebProgressBar()
        loadprogress.isIndeterminate = true
        loadprogress.isStringPainted = true
        loadprogress.string = "正在加载商品信息,请稍等..."
        updateCenterPanel(CenterPanel(loadprogress))
    }

    fun updateCenterPanel(panel: Component) {
        centerPanel.removeAll()
        centerPanel.add(panel)
        centerPanel.revalidate()
        centerPanel.repaint()
    }

    override fun getCenterComponent() = centerPanel


    inner class CashShopItemPane(type: Int) : WebPanel() {

        var currentIndex = 0
        val maxitem = 30
        val items = LinkedList<Pair<ItemPanel, CashItemInfo>>()
        val itemPanel = object : WebPanel(FlowLayout(FlowLayout.CENTER, 20, 5)) {
            init {
                preferredSize = Dimension(1150, 650)
                updateAllItems(CashItemFactory.getInstance().allItem.filter { it -> it.key / 100000 == type })
            }
        }
        var onSaleItems: List<Pair<ItemPanel, CashItemInfo>> = emptyList()
        var onSaleItems_not: List<Pair<ItemPanel, CashItemInfo>> = emptyList()

        val breadcrumb: WebBreadcrumb = object : WebBreadcrumb() {
            init {
                fillBreadcrumb(this, items.size)
            }
        }
        var showType = 0

        init {
            updateFilter()
            val alltype = WebRadioButton("全部", true)
            val uptype = WebRadioButton("已上架")
            val downtype = WebRadioButton("已下架")
//            UnselectableButtonGroup.group(alltype, uptype, downtype)

            alltype.addActionListener(ChangeTypeActionListener(0))
            uptype.addActionListener(ChangeTypeActionListener(1))
            downtype.addActionListener(ChangeTypeActionListener(2))

            SwingUtils.groupButtons(alltype, uptype, downtype)

            add(CenterPanel(GroupPanel(10, alltype, uptype, downtype).setMargin(5, 0, 0, 0)), BorderLayout.NORTH)

            add(itemPanel)

            val navigatBar = GroupPanel(
                    object : WebButton("首页") {
                        init {
                            addActionListener { spawnPange(getSafeIndex(0)) }
                        }
                    },
                    breadcrumb,
                    object : WebButton("尾页") {
                        init {
                            addActionListener { spawnPange(getSafeIndex(getItemList().size)) }
                        }
                    },
                    object : WebButton("上一页") {
                        init {
                            addActionListener { spawnPange(getSafeIndex(currentIndex - 1)) }
                        }
                    },
                    object : WebButton("下一页") {
                        init {
                            addActionListener { spawnPange(getSafeIndex(currentIndex + 1)) }
                        }
                    }
            )
            add(CenterPanel(navigatBar), BorderLayout.SOUTH)
        }

        fun updateAllItems(itemsCache: Map<Int, CashItemInfo>): Unit {
            items.clear()
            val allitems = ArrayList<Map.Entry<Int, CashItemInfo>>(itemsCache.toSortedMap().entries)
            allitems.sortWith(Comparator<Map.Entry<Int, CashItemInfo>> { o1, o2 ->
                o2.value.onSale().compareTo(o1.value.onSale())
            })
            allitems.forEach { it -> items.add(Pair(ItemPanel(this@CashShopItemPane, it.value), it.value)) }
        }

        fun updateFilter() {
            onSaleItems = items.filter { it.second.onSale() }
            onSaleItems_not = items.filter { !it.second.onSale() }
        }

        fun getSafeIndex(index: Int): Int {
            var ret = index
            val safeIndex = getItemList().size / maxitem
            if (index < 0) {
                ret = 0
            } else if (index > safeIndex) {
                ret = safeIndex
            }

            currentIndex = ret
            breadcrumb.components.forEach { it ->
                if (it is WebBreadcrumbToggleButton && it.text == ret.toString()) {
                    it.isSelected = true
                    return@forEach
                }
            }
            return ret
        }

        fun fillBreadcrumb(b: WebBreadcrumb, x: Int) {
            (0..x / maxitem).forEach { i ->
                val button = object : WebBreadcrumbToggleButton("$i") {
                    init {
                        addActionListener { spawnPange(i) }
                    }
                }
                // 默认点击首页
                if (i == 0) button.doClick()
                b.add(button)
            }
            SwingUtils.groupButtons(b)
        }

        fun spawnPange(index: Int) {
            val filterItems = getItemList()
            itemPanel.removeAll()
            ((maxitem * index)..(Math.min(filterItems.size, maxitem * (index + 1)) - 1)).forEach { index -> itemPanel.add(filterItems[index].first) }
            itemPanel.revalidate()
            itemPanel.repaint()
            currentIndex = index
        }

        fun getItemList(): List<Pair<ItemPanel, CashItemInfo>> {
            return if (showType == 1) onSaleItems else if (showType == 2) onSaleItems_not else items
        }

        inner class ChangeTypeActionListener(val type: Int) : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                if (showType != type) {
                    showType = type
                    breadcrumb.removeAll()
                    fillBreadcrumb(breadcrumb, getItemList().size)
                    spawnPange(0)
                }
            }

        }
    }

    inner class ItemPanel(val owner: CashShopItemPane, val cii: CashItemInfo) : WebPanel() {
        val cmi by lazy(mode = LazyThreadSafetyMode.NONE) {
            CashItemFactory.getInstance().getModInfo(cii.sn) ?: CashItemInfo.CashModInfo(cii.sn, cii.price, 0, cii.onSale(), cii.itemId, cii.priority.toInt(), false, cii.period, cii.gender, cii.count, cii.meso, cii.csClass.toInt(), cii.termStart, cii.termEnd, 0, 0, 0, 0, false)
        }
        val itemName_: String by lazy(mode = LazyThreadSafetyMode.NONE) {
            MapleItemInformationProvider.getInstance().getName(cii.itemId)
        }

        val itemName = WebLabel()
        val itemPirce = WebLabel()
        val itemIcon = WebLabel()

        init {
            border = BorderFactory.createMatteBorder(5, 5, 30, 30, Color.GRAY)
            preferredSize = Dimension(200, 95)
            updateLabel(true)
//            val itemOldPirce = WebLabel(if (cii.isDiscount) "${cii.price}点券(${ceilpref(cii.originalPrice, cii.price)}%)" else "无折扣")
            val up_button = WebButton(getOnSale(cmi.isShowUp, true))
            val down_button = WebButton(getOnSale(!cmi.isShowUp, false))
            val change_button = WebButton("更改")

            TooltipManager.setTooltip(this, "<html><div style=\"width:200px\"><center>SN: ${cmi.sn}</center><br><p>${replaceHtmlFormat(MapleItemInformationProvider.getInstance().getDesc(cmi.itemid))}</p></div><html>")

            up_button.isEnabled = !cmi.isShowUp
            down_button.isEnabled = cmi.isShowUp

            // 添加按钮的事件监听
            up_button.addActionListener {
                upOrDown_Action(true)
                up_button.isEnabled = false
                down_button.isEnabled = true
                up_button.text = getOnSale(cmi.isShowUp, true)
                down_button.text = getOnSale(!cmi.isShowUp, false)
                owner.updateFilter()
            }
            down_button.addActionListener {
                upOrDown_Action(false)
                up_button.isEnabled = true
                down_button.isEnabled = false
                up_button.text = getOnSale(cmi.isShowUp, true)
                down_button.text = getOnSale(!cmi.isShowUp, false)
                owner.updateFilter()
            }
            change_button.addActionListener { change_Action() }

            // 修饰Item面板的效果
            isUndecorated = false

            add(itemIcon, BorderLayout.WEST)
            add(GroupPanel(false, itemName, itemPirce).setMargin(0, 10, 10, 0))
            add(CenterPanel(GroupPanel(GroupPanel(5, up_button, down_button, change_button))), BorderLayout.SOUTH)
            setMargin(8)
            round = 10
        }

        fun replaceHtmlFormat(text: String?): String {
            if (text == null || text.isEmpty()) {
                return "无描述"
            }
            var ret = text.replace("\\r\\n", "<br>").replace("\\n", "<br>")
            if (ret.contains("#c")) {
                ret = ret.replace("#c", "<font color=\"orange\">")
                ret = ret.replace("#", "</font>")
            }
            return ret
        }

        fun getOnSale(onSale: Boolean, isUp: Boolean): String {
            return "${(if (onSale) "已" else "")}${if (isUp) "上" else "下"}架"
        }

        fun upOrDown_Action(up: Boolean) {
            cmi.isShowUp = up
            CashItemFactory.getInstance().allModInfo.put(cmi.sn, cmi)
            updateLabel(false)
            updateToDB()
        }

        fun change_Action() {
            val dialog = WebDialog(this@CashShopPane.owner, "名称:$itemName_  SN:${cmi.sn}", true)
            val groupPanel = GroupPanel(5, false)
            val values = CashShopPane.values
            val doubles = DoubleArray(values.size + 1)
            Arrays.fill(doubles, 0, doubles.size, TableLayout.PREFERRED)
            groupPanel.layout = TableLayout(doubleArrayOf(TableLayout.PREFERRED, TableLayout.FILL), doubles, 5, 5)
            val textFields = HashMap<String, Component>()
            var i = 0
            values.forEach { name, any ->
                groupPanel.add(WebLabel(name, WebLabel.TRAILING), "0,$i")
                val compon: Component
                when (any) {
                    is Array<*> -> compon = WebComboBox(any, if (name == "产品类型") cmi.mark else cmi.gender)
                    else -> {
                        val defaultvalue: Int
                        when (name) {
                            "物品ID" -> defaultvalue = cmi.itemid
                            "物品数量" -> defaultvalue = cmi.count
                            "价格" -> defaultvalue = cmi.discountPrice
                            "开始销售时间" -> defaultvalue = cmi.termStart
                            "停止销售时间" -> defaultvalue = cmi.termEnd
                            "等级" -> defaultvalue = cmi.levelLimit
                            "人气" -> defaultvalue = cmi.fameLimit
                            else -> defaultvalue = 0
                        }
                        compon = WebTextField(defaultvalue.toString(), 15)
                    }
                }
                textFields.put(name, compon)
                groupPanel.add(compon, "1,${i++}")
            }
            groupPanel.add(CenterPanel(GroupPanel(5, object : WebButton("确定") {
                init {
                    addActionListener {
                        textFields.forEach {
                            val compoent = it.value
                            when (compoent) {
                                is WebTextField -> {
                                    if (compoent.text.isEmpty()) {
                                        compoent.text = "0"
                                    } else if (!StringUtil.isNumber(compoent.text) || compoent.text.isEmpty()) {
                                        WebOptionPane.showMessageDialog(dialog, "${compoent.text} 不是一个有效的值")
                                        return@addActionListener
                                    }
                                }
                            }
                        }
                        textFields.forEach {
                            val compoent = it.value
                            val text = (compoent as? WebTextField)?.text?.toInt() ?: (compoent as? WebComboBox)?.selectedIndex!!
                            when (it.key) {
                                "物品ID" -> {
                                    cmi.itemid = text
                                    updateLabel(true)
                                }
                                "物品数量" -> {
                                    cmi.count = text
                                    updateLabel(true)
                                }
                                "产品类型" -> cmi.mark = text
                                "价格" -> {
                                    cmi.discountPrice = text
                                    updateLabel(true)
                                }
                                "开始销售时间" -> cmi.termStart = text
                                "停止销售时间" -> cmi.termEnd = text
                                "性别" -> cmi.gender = text
                                "等级" -> cmi.levelLimit = text
                                "人气" -> cmi.fameLimit = text
                            }
                        }
                        cmi.cii = null
                        cmi.toCItem(cii)
                        CashItemFactory.getInstance().allModInfo.put(cmi.sn, cmi)
                        updateToDB()
                        dialog.dispose()
                    }
                }
            }, object : WebButton("取消") {
                init {
                    addActionListener { dialog.dispose() }
                }
            })), "0," + values.size + ",1," + values.size)
            groupPanel.setMargin(10, 20, 10, 20)
            dialog.add(CenterPanel(groupPanel))
            dialog.pack()
            dialog.setLocationRelativeTo(this@CashShopPane)
            dialog.isVisible = true
        }

        fun updateToDB() {
            val con = DatabaseConnection.getInstance().connection
            var ps: PreparedStatement? = null
            var pse: PreparedStatement? = null
            var rs: ResultSet? = null
            try {
                ps = con.prepareStatement("SELECT * FROM `cashshop_modified_items` WHERE `serial` = ?")
                ps.setInt(1, cmi.sn)
                rs = ps.executeQuery()
                // 如果存在数据则只刷新
                if (rs.next()) {
                    pse = con.prepareStatement("UPDATE cashshop_modified_items SET discount_price = ?, mark = ?, showup = ?, itemid = ?, priority = ?, period = ?, gender = ?, count = ?, meso = ?, csClass = ?, termStart = ?, termEnd = ?, fameLimit = ?, levelLimit = ?, categories = ? WHERE serial = ?")
                    pse.setInt(1, cmi.discountPrice)
                    pse.setInt(2, cmi.mark)
                    pse.setByte(3, if (cmi.isShowUp) 1 else 0)
                    pse.setInt(4, cmi.itemid)
                    pse.setByte(5, cmi.priority.toByte())
                    pse.setInt(6, cmi.period)
                    pse.setByte(7, cmi.gender.toByte())
                    pse.setShort(8, cmi.count.toShort())
                    pse.setInt(9, cmi.meso)
                    pse.setByte(10, cmi.csClass.toByte())
                    pse.setInt(11, cmi.termStart)
                    pse.setInt(12, cmi.termEnd)
                    pse.setShort(13, cmi.fameLimit.toShort())
                    pse.setShort(14, cmi.levelLimit.toShort())
                    pse.setByte(15, cmi.categories.toByte())
                    pse.setInt(16, cmi.sn)
                    pse.executeUpdate()
                } else {
                    pse = con.prepareStatement("INSERT INTO cashshop_modified_items(serial, discount_price, mark, showup, itemid, priority, period, gender, count, meso, csClass, termStart, termEnd, fameLimit, levelLimit, categories) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    var index = 0
                    pse.setInt(++index, cmi.sn)
                    pse.setInt(++index, cmi.discountPrice)
                    pse.setInt(++index, cmi.mark)
                    pse.setByte(++index, if (cmi.isShowUp) 1 else 0)
                    pse.setInt(++index, cmi.itemid)
                    pse.setByte(++index, cmi.priority.toByte())
                    pse.setInt(++index, cmi.period)
                    pse.setByte(++index, cmi.gender.toByte())
                    pse.setShort(++index, cmi.count.toShort())
                    pse.setInt(++index, cmi.meso)
                    pse.setByte(++index, cmi.csClass.toByte())
                    pse.setInt(++index, cmi.termStart)
                    pse.setInt(++index, cmi.termEnd)
                    pse.setShort(++index, cmi.fameLimit.toShort())
                    pse.setShort(++index, cmi.levelLimit.toShort())
                    pse.setByte(++index, cmi.categories.toByte())
                    pse.executeUpdate()
                }
            } catch (e: SQLException) {
                log.error("更新商城重载物品数据失败", e)
            } finally {
                ps?.close()
                pse?.close()
                rs?.close()
                con.close()
            }

//            con.prepareStatement("SELECT dropperid FROM drop_data GROUP BY dropperid").use { ps ->
//                ps.executeQuery().use({ rs ->
//                    while (rs.next()) {
//                        mobids.add(rs.getInt("dropperid"))
//                    }
//                })
//            }

        }

        fun ceilpref(x: Int, y: Int): Int {
            return (((x - y) / y.toDouble()) * 100).toInt()
        }

        fun updateLabel(all: Boolean) {
            if (all) {
                val inlink = MapleItemInformationProvider.getInstance().getInLinkID(cmi.itemid)
                itemIcon.icon = loadIcon(if (inlink != 0) inlink else cmi.itemid)
                itemName.text = itemName_
                itemPirce.text = "${cmi.discountPrice}点券(${cmi.count}个)"
            }
            borderColor = if (cmi.isShowUp) Color.magenta else Color.lightGray
//            background = if (cmi.isShowUp) Color.RED else Color.BLACK
//            isWebColoredBackground = true
        }

        fun loadIcon(id: Int) = ImageIcon("config\\icon\\$id.png")
    }
}