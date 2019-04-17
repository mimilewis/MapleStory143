package server.console.groups.setting;

import com.alee.extended.panel.GroupPanel;

import java.awt.*;

public class MonsterConfigGroup extends AbstractConfigGroup {
    MonsterConfigGroup(ConfigPanel owner) {
        super(owner, "怪物相关");
    }

    @Override
    public Component getPreview() {
        TitleWebPanel titleWebPanel1 = new TitleWebPanel("常规设置");
        titleWebPanel1.add(new GroupPanel(false,
                new ConfigComponent("怪物刷新时间(秒)", "channel.monster.refresh"),
                new ConfigComponent("地图最大怪物数量", "channel.monster.maxcount")
        ).setMargin(5));

        TitleWebPanel titleWebPanel2 = new TitleWebPanel("技能设置");
        titleWebPanel2.add(new GroupPanel(5, false,
                new ConfigComponent("关闭怪物技能减益效果(DEBUFF)", ComponentType.复选框, "channel.server.applyplayerdebuff"),
                new ConfigComponent("关闭玩家技能减益效果(DEBUFF)", ComponentType.复选框, "channel.server.applymonsterstatus")
        ).setMargin(5));

        return new GroupPanel(5, false, titleWebPanel1, titleWebPanel2);
    }
}
