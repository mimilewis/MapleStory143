package server;

import constants.BattleConstants;
import constants.BattleConstants.PItem;
import constants.GameConstants;
import tools.Randomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomRewards {

    private static final List<Integer> compiledGold;
    private static final List<Integer> compiledSilver;
    private static final List<Integer> compiledFishing;
    private static final List<Integer> compiledPeanut;
    private static final List<Integer> compiledEvent;
    private static final List<Integer> compiledEventC;
    private static final List<Integer> compiledEventB;
    private static final List<Integer> compiledEventA;
    private static final List<Integer> compiledPokemon;
    private static final List<Integer> compiledDrops;
    private static final List<Integer> compiledDropsB;
    private static final List<Integer> compiledDropsA;
    private static final List<Integer> tenPercent;

    static {
        // 永恒的谜之蛋
        List<Integer> returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.goldrewards);

        compiledGold = returnArray;

        // 重生的谜之蛋
        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.silverrewards);

        compiledSilver = returnArray;

        // Fishing Rewards
        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.fishingReward);

        compiledFishing = returnArray;

        // 获得任务奖励
        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.eventCommonReward);

        compiledEventC = returnArray;

        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.eventUncommonReward);

        compiledEventB = returnArray;

        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.eventRareReward);
        processRewardsSimple(returnArray, GameConstants.tenPercent);
        processRewardsSimple(returnArray, GameConstants.tenPercent);//hack: chance = 2

        compiledEventA = returnArray;

        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.eventSuperReward);

        compiledEvent = returnArray;

        returnArray = new ArrayList<>();

        processRewards(returnArray, GameConstants.peanuts);

        compiledPeanut = returnArray;

        returnArray = new ArrayList<>();

        processPokemon(returnArray, BattleConstants.PokemonItem.values());
        processPokemon(returnArray, BattleConstants.HoldItem.values());

        compiledPokemon = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.normalDrops);

        compiledDrops = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.rareDrops);

        compiledDropsB = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.superDrops);

        compiledDropsA = returnArray;

        returnArray = new ArrayList<>();

        processRewardsSimple(returnArray, GameConstants.tenPercent);

        tenPercent = returnArray;
    }

    private static void processRewards(List<Integer> returnArray, int[] list) {
        int lastitem = 0;
        for (int i = 0; i < list.length; i++) {
            if (i % 2 == 0) { // Even
                lastitem = list[i];
            } else { // Odd
                for (int j = 0; j < list[i]; j++) {
                    returnArray.add(lastitem);
                }
            }
        }
        Collections.shuffle(returnArray);
    }

    private static void processRewardsSimple(List<Integer> returnArray, int[] list) {
        for (int aList : list) {
            returnArray.add(aList);
        }
        Collections.shuffle(returnArray);
    }

    private static void processPokemon(List<Integer> returnArray, PItem[] list) {
        for (PItem lastitem : list) {
            for (int j = 0; j < lastitem.getItemChance(); j++) {
                returnArray.add(lastitem.getId());
            }
        }
        Collections.shuffle(returnArray);
    }

    public static int getGoldBoxReward() {
        return compiledGold.get(Randomizer.nextInt(compiledGold.size()));
    }

    public static int getSilverBoxReward() {
        return compiledSilver.get(Randomizer.nextInt(compiledSilver.size()));
    }

    public static int getFishingReward() {
        return compiledFishing.get(Randomizer.nextInt(compiledFishing.size()));
    }

    public static int getPeanutReward() {
        return compiledPeanut.get(Randomizer.nextInt(compiledPeanut.size()));
    }

    public static int getPokemonReward() {
        return compiledPokemon.get(Randomizer.nextInt(compiledPokemon.size()));
    }

    public static int getEventReward() {
        int chance = Randomizer.nextInt(101);
        if (chance < 66) {
            return compiledEventC.get(Randomizer.nextInt(compiledEventC.size()));
        } else if (chance < 86) {
            return compiledEventB.get(Randomizer.nextInt(compiledEventB.size()));
        } else if (chance < 96) {
            return compiledEventA.get(Randomizer.nextInt(compiledEventA.size()));
        } else {
            return compiledEvent.get(Randomizer.nextInt(compiledEvent.size()));
        }
    }

    public static int getDropReward() {
        int chance = Randomizer.nextInt(101);
        if (chance < 76) {
            return compiledDrops.get(Randomizer.nextInt(compiledDrops.size()));
        } else if (chance < 96) {
            return compiledDropsB.get(Randomizer.nextInt(compiledDropsB.size()));
        } else {
            return compiledDropsA.get(Randomizer.nextInt(compiledDropsA.size()));
        }
    }

    public static List<Integer> getTenPercent() {
        return tenPercent;
    }

    public static void load() {
        //Empty method to initialize class.
    }
}
