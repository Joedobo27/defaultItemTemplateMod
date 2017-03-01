package com.Joedobo27.defaultItemtemplate;


import com.wurmonline.server.DbConnector;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.utils.DbUtilities;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultItemTemplateMod implements WurmServerMod, ItemTemplatesCreatedListener {

    private static final Logger logger;

    static {
        logger = Logger.getLogger(DefaultItemTemplateMod.class.getName());
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
        // foreach modDbTemplates, does it exists in wurmitems.db and it's missing from ItemTemplateFactory#templates. If so add
        // a default template to ItemTemplateFactory#templates.


        // Another way would be to fetch all templates from Wurm. Compare if a fetched itemDB templateid and realTemplates is
        // in the Wurm-templates list. If not add it using the data in the item.DB
        ArrayList<Integer> itemTemplateIDs = new ArrayList<>();
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        fieldTemplates.keySet().forEach(itemTemplateIDs::add);
        ArrayList<TemplateDataStructure> structures = checkIfPresentInItemDb(itemTemplateIDs);
        if (structures.size() > 0) {
            addToWurmTemplates(structures);
        }
    }

    private static void addToWurmTemplates(ArrayList<TemplateDataStructure> structures) throws IOException {
        for (TemplateDataStructure templateDataStructure:structures){
            ItemTemplateFactory.getInstance().createItemTemplate(
                    templateDataStructure.getTemplateID(), 3, "Missing item", "Missing item", "excellent",
                    "good", "ok", "poor","No item template matches this item",
                    new short[]{112, 175}, (short) 340, (short) 1,0, Long.MAX_VALUE, templateDataStructure.getSizeX(), templateDataStructure.getSizeY(),
                    templateDataStructure.getSizeZ(),-10, MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY, "model.writ.deed.", 200.0f,
                    templateDataStructure.getWeight(),
                    (byte) 0, 0, false, -1);
            logger.log(Level.INFO, "Added default template for " + templateDataStructure.getTemplateID());
        }
    }

    private static ArrayList<TemplateDataStructure> checkIfPresentInItemDb(ArrayList<Integer> itemTemplateIDs) throws SQLException {
        ArrayList<TemplateDataStructure> toReturn = new ArrayList<>();
        Connection dbcon = DbConnector.getItemDbCon();
        PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM ITEMS");
        ResultSet rs = ps.executeQuery();

        final int[] itemDbTemplateId = new int[]{0};
        boolean isMissingItemDbTemplate;
        boolean isAbsentFromMissingTemplates;
        while (rs.next()) {
            itemDbTemplateId[0] = rs.getInt("TEMPLATEID");
            isMissingItemDbTemplate =
                    itemTemplateIDs.stream()
                            .filter(value -> Objects.equals(value, itemDbTemplateId[0]))
                            .count()
                            == 0;
            isAbsentFromMissingTemplates = TemplateDataStructure.getDistinctTemplates().stream()
                    .filter(integer -> integer == itemDbTemplateId[0])
                    .count() == 0;
            if (isMissingItemDbTemplate && isAbsentFromMissingTemplates) {
                // grab information from DB about the missing template and add it to return object
                toReturn.add(new TemplateDataStructure(
                        itemDbTemplateId[0], rs.getInt("SIZEX"), rs.getInt("SIZEY"),
                        rs.getInt("SIZEZ"), rs.getInt("WEIGHT")
                ));
            }
        }

        PreparedStatement ps1 = dbcon.prepareStatement("SELECT * FROM ITEMS where TEMPLATEID=669");
        ResultSet rs1 = ps1.executeQuery();
        while (rs1.next()) {
            itemDbTemplateId[0] = rs1.getInt("REALTEMPLATE");
            isMissingItemDbTemplate =
                    itemTemplateIDs.stream()
                            .filter(value -> Objects.equals(value, itemDbTemplateId[0]))
                            .count()
                            == 0;
            isAbsentFromMissingTemplates = TemplateDataStructure.getDistinctTemplates().stream()
                    .filter(integer -> integer == itemDbTemplateId[0])
                    .count() == 0;
            if (isMissingItemDbTemplate && isAbsentFromMissingTemplates) {
                // grab information from DB about the missing template and add it to return object
                toReturn.add(new TemplateDataStructure(itemDbTemplateId[0], rs1.getString("DESCRIPTION"),
                        rs1.getInt("WEIGHT")));
            }
        }
        DbUtilities.closeDatabaseObjects(ps, rs);
        DbUtilities.closeDatabaseObjects(ps1, rs1);
        return toReturn;
    }

    @Deprecated
    private static TemplateDataStructure templateFromBulk(int templateID) throws SQLException {
        Connection dbcon = DbConnector.getItemDbCon();
        PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM ITEMS WHERE TEMPLATEID=? AND REALTEMPLATE=?");
        ps.setInt(1, 669);
        ps.setInt(2, templateID);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            DbUtilities.closeDatabaseObjects(ps, rs);
            return null;
        }
        TemplateDataStructure templateDataStructure = new TemplateDataStructure(templateID, rs.getString("DESCRIPTION"), rs.getInt("WEIGHT"));
        DbUtilities.closeDatabaseObjects(ps, rs);
        return templateDataStructure;
    }

    @Deprecated
    private static TemplateDataStructure searchTemplateID(int templateID) throws SQLException{
        Connection dbcon = DbConnector.getItemDbCon();
        PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM ITEMS WHERE TEMPLATEID=?");
        ps.setInt(1, templateID);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            DbUtilities.closeDatabaseObjects(ps, rs);
            return null;
        }
        TemplateDataStructure templateDataStructure = new TemplateDataStructure(templateID, rs.getInt("SIZEX"), rs.getInt("SIZEY"), rs.getInt("SIZEZ"), rs.getInt("WEIGHT"));
        DbUtilities.closeDatabaseObjects(ps, rs);
        return templateDataStructure;
    }
}
