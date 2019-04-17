package server.console.groups.setting;

import com.alee.extended.panel.GroupPanel;

import java.awt.*;

public class OnlineRewardConfigGroup extends AbstractConfigGroup {
    OnlineRewardConfigGroup(ConfigPanel owner) {
        super(owner, "在线奖励");
    }

    @Override
    public Component getPreview() {
        TitleWebPanel titleWebPanel1 = new TitleWebPanel("在线奖励设置");
        titleWebPanel1.add(new GroupPanel(false,
                new ConfigComponent("开启在线奖励", ComponentType.复选框, "channel.reward.isopen"),
                new ConfigComponent("奖励间隔时间(秒)", "channel.reward.refreshtime"),
                new ConfigComponent("获得经验数量", "channel.reward.exp"),
                new ConfigComponent("获得点券数量", "channel.reward.acash"),
                new ConfigComponent("获得抵用券数量", "channel.reward.mpoint"),
                new ConfigComponent("获得金币数量", "channel.reward.meso"),
                new ConfigComponent("获得积分数量", "channel.reward.integral"),
                new ConfigComponent("获得元宝数量", "channel.reward.rmb")
        ).setMargin(5));
        TitleWebPanel titleWebPanel2 = new TitleWebPanel("其他设置");
        titleWebPanel2.add(new GroupPanel(false,
                new ConfigComponent("打怪获得点券(为0则关闭)", "channel.monster.givepoint")
        ).setMargin(5));
        return new GroupPanel(5, false, titleWebPanel1, titleWebPanel2);
    }
}
