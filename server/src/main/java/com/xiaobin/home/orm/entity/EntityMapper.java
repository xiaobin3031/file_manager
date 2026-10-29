package com.xiaobin.home.orm.entity;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface EntityMapper {

    <T> T map(ResultSet rs, EntityMeta meta) throws SQLException;
}
