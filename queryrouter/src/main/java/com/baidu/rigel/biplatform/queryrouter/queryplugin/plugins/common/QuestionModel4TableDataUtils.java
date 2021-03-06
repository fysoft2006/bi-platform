/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.rigel.biplatform.queryrouter.queryplugin.plugins.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.CollectionUtils;

import com.baidu.rigel.biplatform.ac.minicube.CallbackLevel;
import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ac.model.DimensionType;
import com.baidu.rigel.biplatform.ac.model.Level;
import com.baidu.rigel.biplatform.ac.query.model.AxisMeta;
import com.baidu.rigel.biplatform.ac.query.model.AxisMeta.AxisType;
import com.baidu.rigel.biplatform.ac.query.model.ConfigQuestionModel;
import com.baidu.rigel.biplatform.ac.query.model.QuestionModel;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.plugins.model.SqlColumn;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.plugins.model.SqlConstants;

/**
 * QuestionModel to TableData的工具类
 * 
 * @author luowenlei
 *
 */
public class QuestionModel4TableDataUtils {

    /**
     * 获取questionModel中需要查询的Columns
     * 
     * @param AxisMetas
     *            axisMetas map
     * 
     * @return List needcolumns hashmap
     */
    private static List<SqlColumn> getNeedColumns(
            Map<String, SqlColumn> allColums,
            Map<AxisType, AxisMeta> axisMetas) {
        Set<SqlColumn> needColumns = new HashSet<SqlColumn>();
        if (CollectionUtils.isEmpty(allColums)) {
            return new ArrayList<SqlColumn>(needColumns);
        }
        // 获取指标元数据
        AxisMeta axisMetaMeasures = (AxisMeta) axisMetas.get(AxisType.COLUMN);
        axisMetaMeasures.getQueryMeasures().forEach((measureName) -> {
            needColumns.add(allColums.get(measureName));
        });

        // 获取指标元数据
        axisMetaMeasures.getCrossjoinDims().forEach(
                (dimName) -> {
                    needColumns.add(allColums.get(dimName));
                });

        // 获取维度元数据
        AxisMeta axisMetaDims = (AxisMeta) axisMetas.get(AxisType.ROW);
        axisMetaDims.getCrossjoinDims().forEach(
                (dimName) -> {
                    needColumns.add(allColums.get(dimName));
                });
        return new ArrayList<SqlColumn>(needColumns);
    }

    /**
     * 获取questionModel中需要查询的Columns
     * 
     * @param QuestionModel
     *            questionModel
     * 
     * @return List needcolumns hashmap
     */
    public static List<SqlColumn> getNeedColumns(QuestionModel questionModel) {
        Map<String, SqlColumn> allColums = QuestionModel4TableDataUtils.getAllCubeColumns(questionModel);
        Map<AxisType, AxisMeta> axisMetas = questionModel.getAxisMetas();
        return QuestionModel4TableDataUtils.getNeedColumns(allColums, axisMetas);
    }

    /**
     * 获取指标及维度中所有的字段信息Formcube
     * 
     * @param cube
     *            cube
     * @return HashMap allcolumns hashmap
     */
    public static HashMap<String, SqlColumn> getAllCubeColumns(
            QuestionModel questionModel) {
        ConfigQuestionModel configQuestionModel = (ConfigQuestionModel) questionModel;
        MiniCube miniCube = (MiniCube) configQuestionModel.getCube();
        HashMap<String, SqlColumn> allColumns = new HashMap<String, SqlColumn>();
        // 获取指标元数据
        miniCube.getMeasures().forEach(
                (k, v) -> {
                    String measureName = k;
                    allColumns.put(measureName, new SqlColumn());
                    SqlColumn oneMeasure = allColumns.get(measureName);
                    oneMeasure.setName(measureName);
                    oneMeasure.setTableFieldName(v.getDefine());
                    oneMeasure.setCaption(v.getCaption());
                    oneMeasure.setTableName(QuestionModel4TableDataUtils
                            .getFactTableAliasName());
                    oneMeasure.setSourceTableName(miniCube.getSource());
                    oneMeasure.setType(AxisType.COLUMN);
                    oneMeasure.setSqlUniqueColumn(QuestionModel4TableDataUtils
                            .getFactTableAliasName()
                            + v.getDefine());
                    oneMeasure.setMeasure(v);
                    oneMeasure.setColumnKey(System.nanoTime() + oneMeasure.getTableFieldName());
                });

        // 获取维度元数据
        miniCube.getDimensions()
                .forEach(
                        (k, v) -> {
                            Level oneDimensionSource = (Level) v.getLevels()
                                    .values().toArray()[0];
                            allColumns.put(k, new SqlColumn());
                            SqlColumn oneDimensionTarget = allColumns.get(k);
                            String name = k;
                            String tableFieldName = oneDimensionSource
                                    .getName();
                            Dimension dimension = miniCube.getDimensions().get(
                                    name);
                            String tableName = null;
                            String sourceTableName = null;
                            if (QuestionModel4TableDataUtils
                                    .isTimeOrCallbackDimension(dimension)) {
                                // 如果为时间维度，转换成事实表的时间字段
                                name = oneDimensionSource
                                        .getFactTableColumn();
                                tableFieldName = oneDimensionSource
                                        .getFactTableColumn();
                                tableName = QuestionModel4TableDataUtils
                                        .getFactTableAliasName();
                                sourceTableName = miniCube.getSource();
                            } else {
                                tableName = oneDimensionSource.getDimTable();
                                sourceTableName = tableName;
                            }
                            oneDimensionTarget.setName(name);
                            oneDimensionTarget
                                    .setTableFieldName(tableFieldName);
                            oneDimensionTarget.setCaption(oneDimensionSource
                                    .getCaption());
                            oneDimensionTarget.setTableName(tableName);
                            oneDimensionTarget.setSourceTableName(sourceTableName);
                            oneDimensionTarget.setType(AxisType.ROW);
                            oneDimensionTarget.setDimension(v);
                            oneDimensionTarget.setLevel(oneDimensionSource);
                            oneDimensionTarget.setSqlUniqueColumn((tableName + k).toLowerCase());
                            oneDimensionTarget.setColumnKey(System.nanoTime()
                                    + oneDimensionTarget.getTableFieldName());
                            
                        });

        return allColumns;
    }

    /**
     * 获取事实表
     * 
     * @return
     */
    public static String getFactTableAliasName() {
        return SqlConstants.FACTTABLE_ALIAS_NAME;
    }

    /**
     * isTimeOrCallbackDimension
     * 
     * @param dimension
     *            dimension
     * @return isTimeOrCallbackDimension
     */
    public static boolean isTimeOrCallbackDimension(Dimension dimension) {
        if (dimension.getType() == DimensionType.TIME_DIMENSION) {
            // 判断是否是timedimension维度
            return true;
        }
        if (!dimension.getLevels().isEmpty()) {
            if (dimension.getLevels().entrySet().iterator().next().getValue() instanceof CallbackLevel) {
                // 判断是否是callback维度
                return true;
            }
        }
        return false;
    }
}
