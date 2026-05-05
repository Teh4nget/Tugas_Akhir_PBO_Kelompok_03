package beastclash.model;

public class Beast {
    private String name;
    private String element;
    private int maxHP;
    private int currentHP;
    private int maxMana;
    private int currentMana;
    private int attack;
    private int defense;
    private int speed;
    private int id;

    // Rantai Kelemahan Elemen:
    // Api   kuat vs Daun,  lemah vs Air
    // Air   kuat vs Api,   lemah vs Tanah
    // Tanah kuat vs Air,   lemah vs Daun
    // Daun  kuat vs Tanah, lemah vs Api
    // Cahaya dan Gelap saling melemahkan (dua arah 1.5x)

    public Beast(int id, String name, String element, int maxHP, int maxMana, int attack, int defense, int speed) {
        this.id = id;
        this.name = name;
        this.element = element;
        this.maxHP = maxHP;
        this.currentHP = maxHP;
        this.maxMana = maxMana;
        this.currentMana = maxMana;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
    }

    public void takeDamage(int damage) {
        currentHP = Math.max(0, currentHP - damage);
    }

    public void useMana(int amount) {
        currentMana = Math.max(0, currentMana - amount);
    }

    public void restoreMana(int amount) {
        currentMana = Math.min(maxMana, currentMana + amount);
    }

    public void restoreHP(int amount) {
        currentHP = Math.min(maxHP, currentHP + amount);
    }

    public boolean isAlive() {
        return currentHP > 0;
    }

    public void reset() {
        this.currentHP = maxHP;
        this.currentMana = maxMana;
    }

    public double getElementMultiplier(String targetElement) {
        // Siklus: Api > Daun > Tanah > Air > Api
        // Cahaya dan Gelap saling melemahkan dua arah
        switch (this.element) {
            case "Api":
                if (targetElement.equals("Daun"))   return 1.5; // Api KUAT lawan Daun
                if (targetElement.equals("Air"))    return 0.5; // Api LEMAH lawan Air
                break;
            case "Air":
                if (targetElement.equals("Api"))    return 1.5; // Air KUAT lawan Api
                if (targetElement.equals("Tanah"))  return 0.5; // Air LEMAH lawan Tanah
                break;
            case "Tanah":
                if (targetElement.equals("Air"))    return 1.5; // Tanah KUAT lawan Air
                if (targetElement.equals("Daun"))   return 0.5; // Tanah LEMAH lawan Daun
                break;
            case "Daun":
                if (targetElement.equals("Tanah"))  return 1.5; // Daun KUAT lawan Tanah
                if (targetElement.equals("Api"))    return 0.5; // Daun LEMAH lawan Api
                break;
            case "Cahaya":
                if (targetElement.equals("Gelap"))  return 1.5; // Cahaya KUAT lawan Gelap
                break;
            case "Gelap":
                if (targetElement.equals("Cahaya")) return 1.5; // Gelap KUAT lawan Cahaya
                break;
        }
        return 1.0;
    }

    public int calculateAttackDamage(Beast target) {
        double mult = getElementMultiplier(target.getElement());
        int raw = Math.max(1, this.attack - target.getDefense() / 2);
        return (int)(raw * mult);
    }

    public int calculateSkillDamage(Beast target) {
        double mult = getElementMultiplier(target.getElement());
        int raw = Math.max(1, (int)(this.attack * 1.5) - target.getDefense() / 2);
        return (int)(raw * mult);
    }

    public int calculateUltimateDamage(Beast target) {
        double mult = getElementMultiplier(target.getElement());
        int raw = Math.max(1, this.attack * 2 - target.getDefense() / 3);
        return (int)(raw * mult);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getElement() { return element; }
    public int getMaxHP() { return maxHP; }
    public int getCurrentHP() { return currentHP; }
    public int getMaxMana() { return maxMana; }
    public int getCurrentMana() { return currentMana; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }

    @Override
    public String toString() {
        return name + " [" + element + "] HP:" + currentHP + "/" + maxHP;
    }
}
