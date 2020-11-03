/**
 *
 */
package org.theseed.web.rna;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * @author Bruce Parrello
 *
 */
public class TestColDescriptor {

    @Test
    public void testDelete() {
        String cookieString = "A,;B,B;C,;D,;E,E";
        String cookie2 = ColumnDescriptor.deleteColumn(cookieString, 2);
        assertThat(cookie2, equalTo("A,;B,B;D,;E,E"));
        cookie2 = ColumnDescriptor.deleteColumn(cookieString, 0);
        assertThat(cookie2, equalTo("B,B;C,;D,;E,E"));
        cookie2 = ColumnDescriptor.deleteColumn(cookieString, 6);
        assertThat(cookie2, equalTo(cookieString));
        cookie2 = ColumnDescriptor.deleteColumn(cookieString, 1);
        assertThat(cookie2, equalTo("A,;C,;D,;E,E"));
    }

}
