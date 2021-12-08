/**
 *
 */
package org.theseed.web.rna;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestColDescriptor {

    @Test
    public void testDelete() {
        File workDir = new File("data", "clusters");
        RnaDataType[] rnaTypes = RnaDataType.values(workDir);
        String cookieString = "A,;B,B;C,;D,;E,E";
        assertThat(ColumnDescriptor.getDbType(cookieString, rnaTypes), equalTo(rnaTypes[0]));
        assertThat(ColumnDescriptor.getSortCol(cookieString), equalTo(0));
        String cookie2 = ColumnDescriptor.deleteColumn(cookieString, 2);
        assertThat(cookie2, equalTo("A,;B,B;D,;E,E"));
        cookie2 = ColumnDescriptor.deleteColumn(cookieString, 0);
        assertThat(cookie2, equalTo("B,B;C,;D,;E,E"));
        cookie2 = ColumnDescriptor.deleteColumn(cookieString, 6);
        assertThat(cookie2, equalTo(cookieString));
        cookie2 = ColumnDescriptor.deleteColumn(cookieString, 1);
        assertThat(cookie2, equalTo("A,;C,;D,;E,E"));
    }

    @Test
    public void testCookieParse() {
        String cookieString = "A,B;C,D|10";
        File workDir = new File("data", "clusters");
        RnaDataType[] rnaTypes = RnaDataType.values(workDir);
        assertThat(ColumnDescriptor.getSortCol(cookieString), equalTo(10));
        assertThat(ColumnDescriptor.getDbType(cookieString, rnaTypes), equalTo(rnaTypes[0]));
        cookieString = "C,D;E,|10|1";
        assertThat(ColumnDescriptor.getDbType(cookieString, rnaTypes), equalTo(rnaTypes[1]));
    }

}
