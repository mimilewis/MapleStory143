/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package configs;

import tools.config.Property;

public class NebuliteConfig {

    @Property(key = "server.benulite.C", defaultValue = "10")
    public static String benuliteC;

    @Property(key = "server.benulite.B", defaultValue = "10")
    public static String benuliteB;

    @Property(key = "server.benulite.A", defaultValue = "10")
    public static String benuliteA;

    @Property(key = "server.benulite.S", defaultValue = "10")
    public static String benuliteS;
}
