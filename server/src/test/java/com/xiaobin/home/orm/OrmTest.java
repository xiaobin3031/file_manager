package com.xiaobin.home.orm;

import com.xiaobin.home.HomeApplication;
import com.xiaobin.home.entity.FmNode;
import com.xiaobin.home.orm.query.OrmQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = HomeApplication.class)
@ActiveProfiles("dev")
public class OrmTest {

    @Autowired
    private Orm orm;

    @Test
    public void testSave() {
        FmNode node = new FmNode();
        node.setNodeType((short) 1);
        node.setNodeName("test");
        node.setParentId(0L);
        node.setOwnerId(1);
        node.setStorageId(1);
        int r = orm.save(node);
        Assertions.assertEquals(1, r);
    }

    @Test
    public void testQuery() {
        OrmQuery<FmNode> query = new OrmQuery<>(FmNode.class);
        query.eq(FmNode::getNodeName, "test");

        FmNode one = orm.getOne(query);
        Assertions.assertNotNull(one);
    }

    @Test
    public void testUpdate() {
        OrmQuery<FmNode> query = new OrmQuery<>(FmNode.class);
        query.eq(FmNode::getNodeName, "test");

        FmNode one = orm.getOne(query);
        Assertions.assertNotNull(one);

        one.setNodeName("test2");
        this.orm.updateById(one);

        query = new OrmQuery<>(FmNode.class);
        query.eq(FmNode::getNodeName, "test2");

        one = orm.getOne(query);
        Assertions.assertNotNull(one);
        Assertions.assertEquals("test2", one.getNodeName());
    }
}
