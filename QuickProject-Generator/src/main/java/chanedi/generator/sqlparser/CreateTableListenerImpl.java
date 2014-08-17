package chanedi.generator.sqlparser;

import chanedi.generator.GlobalConfig;
import chanedi.generator.PropertyTypeContext;
import chanedi.generator.model.Bean;
import chanedi.generator.model.Property;
import chanedi.generator.model.PropertyType;
import chanedi.generator.sqlparser.gen.CreateTableBaseListener;
import chanedi.generator.sqlparser.gen.CreateTableParser;
import chanedi.util.StringUtils;
import lombok.Getter;
import org.antlr.v4.runtime.misc.NotNull;
import org.mobicents.commons.annotations.NotThreadSafe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides an empty implementation of {@link chanedi.generator.sqlparser.gen.CreateTableListener},
 * which can be extended to create a listener which only needs to handle a subset
 * of the available methods.
 */
@NotThreadSafe
public class CreateTableListenerImpl extends CreateTableBaseListener {

    @Getter
    private Map<String, Bean> tableMap = new HashMap<String, Bean>(); // key 为 tableName
    private GlobalConfig globalConfig;
    private Bean currentBean;
    private CommentType currentCommentType;

    public CreateTableListenerImpl(GlobalConfig globalConfig) {
        super();
        this.globalConfig = globalConfig;
    }

    public java.util.Collection<Bean> getTables() {
        return tableMap.values();
    }

    @Override
    public void enterMdl(@NotNull CreateTableParser.MdlContext ctx) {
        currentBean = new Bean();
        currentBean.setTableName(ctx.table_name().getText());
        String beanNameRegex = globalConfig.getBeanNameRegex();
        if (beanNameRegex != null) {
            // 根据用户设置修正beanName
            Pattern pattern = Pattern.compile(beanNameRegex);
            Matcher matcher = pattern.matcher(currentBean.getTableName());
            matcher.find();
            String group = matcher.group(matcher.groupCount());
            currentBean.setName(StringUtils.uncapitalizeCamelBySeparator(group, "_"));
        }
        tableMap.put(currentBean.getTableName(), currentBean);
    }

    @Override
    public void enterColumn_definition(@NotNull CreateTableParser.Column_definitionContext ctx) {
        Property column = new Property();
        column.setColumnName(ctx.column_name().getText());
        column.setType(PropertyTypeContext.matchPropertyType(ctx.datatype().getText()));
        currentBean.addProperty(column);
    }

    @Override
    public void enterComment(@NotNull CreateTableParser.CommentContext ctx) {
        String beanTableName = ctx.table_name().getText();
        Bean bean = tableMap.get(beanTableName);
        String comment = ctx.comment_value().getText().replaceAll("'", "");

        CreateTableParser.Column_nameContext column_nameContext = ctx.column_name();
        if (column_nameContext == null) {
            bean.setComment(comment);
        } else {
            String columnName = column_nameContext.getText();
            bean.getPropertyByColumnName(columnName).setComment(comment);
        }

    }

    @Override
    public void enterComment_value(@NotNull CreateTableParser.Comment_valueContext ctx) {
        super.enterComment_value(ctx);
    }

    /*
stringType : 'VARCHAR2' RANGE;
dateType : 'TIMESTAMP' | 'DATE';
doubleType : 'NUMERIC' '(' NUMBER ',' NUMBER ')';
intType : 'NUMERIC' (RANGE | '(' NUMBER ',''0)');
booleanType : 'CHAR(1)';
*/

    private enum CommentType {
        TABLE, COLUMN;
    }

}
