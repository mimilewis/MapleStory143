/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.api;

public class Console {

    private static final Kernel32 kernel32 = Kernel32.INSTANCE;
    private static final int hStdout = kernel32.GetStdHandle(-11);

    /**
     * 设置控制台窗口标题
     *
     * @param title
     */
    public static void setTitle(String title) {
        kernel32.SetConsoleTitleA(title);
    }

    /**
     * 控制台清屏
     */
    public static void clear() {
        PCONSOLE_SCREEN_BUFFER_INFO buff = new PCONSOLE_SCREEN_BUFFER_INFO();
        kernel32.GetConsoleScreenBufferInfo(hStdout, buff);
        int dwConSize = buff.dwSize.x * buff.dwSize.y;
        kernel32.FillConsoleOutputCharacterA(hStdout, 32, dwConSize, 0, new int[1]);
        kernel32.GetConsoleScreenBufferInfo(hStdout, buff);
        kernel32.FillConsoleOutputAttribute(hStdout, buff.wAttributes, dwConSize, 0, new int[1]);
        kernel32.SetConsoleCursorPosition(hStdout, 0);
    }

    /**
     * 设置输出颜色
     * 10 = 绿色
     * 11 = 蓝色
     * 12 = 红色
     * 13 = 粉色
     * 14 = 黄色
     * 15 = 白色
     */
    public static void setColor(int color) {
        kernel32.SetConsoleTextAttribute(hStdout, color);
    }

    /*
     * [ 0x?? 中 ? 的取值 ] 例如 0x12 = 背景为蓝色,前景为绿色 设置默认的控制台前景和背景颜色。 
     * 
     * attr 指定控制台输出的颜色属性
     *
     * 颜色属性由两个十六进制数字指定 -- 第一个为背景，第二个则为 前景。每个数字可以为以下任何值之一:
     *
     * 0 = 黑色 8 = 灰色 1 = 蓝色 9 = 淡蓝色 2 = 绿色 A = 淡绿色 3 = 湖蓝色 B = 淡浅绿色 4 = 红色 C = 淡红色 5 = 紫色 D = 淡紫色 6 = 黄色 E = 淡黄色 7 = 白色 F = 亮白色
     *
     * 。这个值来自当前控制台窗口、/T 开关或DefaultColor 注册表值。
     */
}
