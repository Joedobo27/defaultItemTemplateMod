package com.Joedobo27.defaultItemtemplate;

import java.util.ArrayList;

class TemplateDataStructure {
    private Integer sizeX;
    private Integer sizeY;
    private Integer sizeZ;
    private Integer weight;
    private Integer templateID;
    private static ArrayList<Integer> distinctTemplates;

    static {
        distinctTemplates = new ArrayList<>();
    }

    TemplateDataStructure(int templateID, int sizeX, int sizeY, int sizeZ, int weight) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.weight = weight;
        this.templateID = templateID;
        boolean isDistinct = distinctTemplates.stream().filter(integer -> integer == templateID).count() == 0;
        if (isDistinct)
            distinctTemplates.add(templateID);
    }

    TemplateDataStructure(int templateID, String description, int volume){
        description = description.replaceAll("x", "");
        this.templateID = templateID;
        this.sizeX = 1;
        this.sizeY = 1;
        this.sizeZ = volume/Integer.valueOf(description);
        this.weight = 50000;
        boolean isDistinct = distinctTemplates.stream().filter(integer -> integer == templateID).count() == 0;
        if (isDistinct)
            distinctTemplates.add(templateID);

        //this.deriveXYZ(Integer.parseInt(description.replaceAll("x", ""), 10), volume);
    }

    Integer getTemplateID() {
        return templateID;
    }

    Integer getSizeX() {
        return sizeX;
    }

    Integer getSizeY() {
        return sizeY;
    }

    Integer getSizeZ() {
        return sizeZ;
    }

    Integer getWeight() {
        return weight;
    }

    static ArrayList<Integer> getDistinctTemplates() {
        return distinctTemplates;
    }

    @Deprecated
    private void deriveXYZ(int count, int volumeSummed){
        // 1. find the cube root of single item's volume.
        int volumeSingle = Math.floorDiv(volumeSummed, count);
        double cubedReference = Math.cbrt(volumeSingle);
        // 2. find the first factorial of volume that >= cube value
        ArrayList<Integer> volumeFactors = integerFactoring(volumeSingle);
        int zDimension = 1;
        for (int var: volumeFactors){
            zDimension = var >= cubedReference ? var : 1;
            if (zDimension > 1)
                break;
        }
        this.sizeZ = zDimension;
        // 3. find the first factorial of volume/#2 result that >= cube value
        ArrayList<Integer> volumeFactors2 = integerFactoring(volumeSingle/zDimension);
        int yDimension = 1;
        for (int var: volumeFactors2){
            yDimension = var >= cubedReference ? var : 1;
            if (yDimension > 1)
                break;
        }
        this.sizeY = yDimension;
        // 4. whatever factorial completes #3.
        int xDimension = volumeSingle / zDimension / yDimension;
        if (volumeSingle % xDimension != 0)
            //throw new UnsupportedOperationException();
            this.sizeX = xDimension;
        this.weight = 20000;
    }

    @Override @SuppressWarnings("unused")
    public String toString(){
        return "templateID: " + templateID + ", sizeX: " + this.sizeX + ", sizeY: " + this.sizeY + ", sizeZ: " + this.sizeZ
                + ", weight: " + this.weight;
    }

    /**
     * Factoring an integer, n, by this calculator is done by trial division. First find the square root of n and
     * round it up to the next integer. Let the result equal s.  Test all integers from 1 through s and record all
     * that are divisible into n.
     *
     * @param integer type int.
     * @return ArrayList of int.
     */
    @Deprecated
    private ArrayList<Integer> integerFactoring(int integer){
        double s = Math.ceil(Math.sqrt(integer));
        ArrayList<Integer> factors = new ArrayList<>();
        for (int ind = 2;ind<=s;ind++){
            if (s % ind == 0){
                factors.add(ind);
            }
        }
        return factors;
    }

}
