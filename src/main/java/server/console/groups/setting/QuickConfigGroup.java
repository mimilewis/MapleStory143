package server.console.groups.setting;

import com.alee.extended.panel.GroupPanel;
import com.alee.extended.panel.GroupingType;

import java.awt.*;

public class QuickConfigGroup extends AbstractConfigGroup {


    QuickConfigGroup(ConfigPanel owner) {
        super(owner, "快捷功能配置");
    }

    @Override
    public Component getPreview() {

        TitleWebPanel titleWebPanel1 = new TitleWebPanel("快捷功能");
        titleWebPanel1.add(new GroupPanel(3, false,
                new ConfigComponent("启用管理登陆模式", ComponentType.复选框, "world.server.onlyadmin"),
                new ConfigComponent("启用删除角色功能", ComponentType.复选框, "login.server.deletecharacter"),
                new ConfigComponent("启用自动封号系统", ComponentType.复选框, "world.autoban"),
                new ConfigComponent("启用攻击防御系统", ComponentType.复选框),
                new ConfigComponent("启用挂机检测系统", ComponentType.复选框),
                new ConfigComponent("启用密码加密", ComponentType.复选框, "login.server.usesha1hash")
        ).setMargin(5));


        TitleWebPanel titleWebPanel2 = new TitleWebPanel("其他功能");
        titleWebPanel2.add(new GroupPanel(false,
                new ConfigComponent("任务提示信息", ComponentType.复选框),
                new ConfigComponent("检测玩家能力值", ComponentType.复选框),  //"world.checkplayersp"
                new ConfigComponent("检测玩家点券", ComponentType.复选框),   //"world.checkplayernx"
                new ConfigComponent("检测复制装备", ComponentType.复选框),   //"world.checkplayerequip"
                new ConfigComponent("禁止释放技能", ComponentType.复选框, "world.banallskill"),
                new ConfigComponent("禁止获得经验", ComponentType.复选框, "world.bangainexp"),
                new ConfigComponent("禁止所有交易", ComponentType.复选框, "world.bantrade"),
                new ConfigComponent("禁止怪物爆物", ComponentType.复选框, "world.bandropitem"),
                new ConfigComponent("玩家人气小于0不允许穿戴装备", ComponentType.复选框, "world.equipcheckfame")
        ).setMargin(5));

        return new GroupPanel(GroupingType.fillAll, 5, titleWebPanel1, titleWebPanel2);
    }
}
