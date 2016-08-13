package com.Joedobo27.defaultItemtemplate;


import com.sun.istack.internal.Nullable;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.utils.DbUtilities;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultItemTemplateMod implements WurmServerMod, Configurable, ItemTemplatesCreatedListener {

    private static int[] convertToDefault;
    private static DefaultItemTemplateMod instance;

    private static final Logger logger = Logger.getLogger(DefaultItemTemplateMod.class.getName());

    private static DefaultItemTemplateMod getInstance() {
        if (DefaultItemTemplateMod.instance == null) {
            DefaultItemTemplateMod.instance = new DefaultItemTemplateMod();
        }
        return DefaultItemTemplateMod.instance;
    }

    @Override
    public void configure(Properties properties) {
        convertToDefault = Arrays.stream(properties.getProperty("convertToDefault", Arrays.toString(convertToDefault)).replaceAll("\\s", "").split(",")).mapToInt(Integer::parseInt).toArray();
        logger.log(Level.INFO, Arrays.toString(convertToDefault));
    }

    @Override
    public void onItemTemplatesCreated() {
        try {
            addDefaultTemplates();
        }catch (NoSuchFieldException | IllegalAccessException | IOException | SQLException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }
    }

    private static void addDefaultTemplates() throws NoSuchFieldException, IllegalAccessException, IOException, SQLException {
        if(convertToDefault.length < 1)
            return;
        ArrayList<TemplateDataStructure> toMakeDefaultTemplates = new ArrayList<>();
        for (int templateID : convertToDefault) {
            TemplateDataStructure a = searchTemplateID(templateID);
            TemplateDataStructure b = a == null ? templateFromBulk(templateID) : a;
            if (b == null) {
                logger.log(Level.INFO, "Template: " + templateID + " exists as neither stand alone or a bulk item.");
            }else{
                logger.log(Level.INFO, b.toString());
                toMakeDefaultTemplates.add(b);
            }
        }

        ArrayList<Integer> itemTemplateIDs = new ArrayList<>();
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        itemTemplateIDs.addAll(fieldTemplates.keySet());
        for (int a : convertToDefault) {
            if (!itemTemplateIDs.contains(a)) {
                ItemTemplateFactory.getInstance().createItemTemplate(a, 3, "Missing item", "Missing item", "excellent", "good", "ok", "poor",
                        "No item template matches this item", new short[]{}, (short) 340, (short) 1,
                        0, Long.MAX_VALUE, 1, 1, 1, -10, MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY,
                        "model.writ.deed.", 200.0f, 1, (byte) 0, 0, false, -1);
                logger.log(Level.INFO, "added " + a);
            }
        }
    }

    @Nullable
    private static TemplateDataStructure templateFromBulk(int templateID) throws SQLException{
        Connection dbcon = DbConnector.getItemDbCon();
        PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM ITEMS WHERE TEMPLATEID=? AND REALTEMPLATE=?");
        ps.setInt(1, 669);
        ps.setInt(2, templateID);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            DbUtilities.closeDatabaseObjects(ps, rs);
            return null;
        }
        TemplateDataStructure templateDataStructure = getInstance().new TemplateDataStructure(templateID, rs.getString("DESCRIPTION"), rs.getInt("WEIGHT"));
        DbUtilities.closeDatabaseObjects(ps, rs);
        return templateDataStructure;
    }

    @Nullable
    private static TemplateDataStructure searchTemplateID(int templateID) throws SQLException{
        Connection dbcon = DbConnector.getItemDbCon();
        PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM ITEMS WHERE TEMPLATEID=?");
        ps.setInt(1, templateID);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            DbUtilities.closeDatabaseObjects(ps, rs);
            return null;
        }
        TemplateDataStructure templateDataStructure = getInstance().new TemplateDataStructure(templateID, rs.getInt("SIZEX"), rs.getInt("SIZEY"), rs.getInt("SIZEZ"), rs.getInt("WEIGHT"));
        DbUtilities.closeDatabaseObjects(ps, rs);
        return templateDataStructure;
    }

    private class TemplateDataStructure {
        Integer sizeX;
        Integer sizeY;
        Integer sizeZ;
        Integer weight;
        Integer templateID;

        TemplateDataStructure(int aTemplateID, int aSizeX, int aSizeY, int aSizeZ, int aWeight) {
            this.sizeX = aSizeX;
            this.sizeY = aSizeY;
            this.sizeZ = aSizeZ;
            this.weight = aWeight;
            this.templateID = aTemplateID;
        }

        TemplateDataStructure(int aTemplateID, String aDescription, int aWeight){
            this.templateID = aTemplateID;
            this.deriveXYZ(Integer.parseInt(aDescription.replaceAll("x", ""), 10), aWeight);
        }

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
}
