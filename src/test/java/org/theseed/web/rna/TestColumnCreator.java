/**
 *
 */
package org.theseed.web.rna;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * Test the new-column creation methods.
 *
 * @author Bruce Parrello
 *
 */
public class TestColumnCreator {

    private static final String[] SAMPLES = new String[] {
            "7_0_0_A_asdO_000_D000_0_3_M1", "7_0_0_A_asdO_000_D000_0_4p5_M1", "7_0_0_A_asdO_000_D000_0_4p5_M1_rep1",
            "7_0_0_A_asdO_000_D000_0_5p5_M1", "7_0_0_A_asdO_000_D000_0_9_M1", "7_0_0_A_asdO_000_D000_0_9_M1_rep0",
            "7_0_0_A_asdO_000_D000_0_12_M1", "7_0_0_A_asdO_000_D000_0_24_M1", "7_0_0_A_asdO_000_D000_0_24_M1_rep0",
            "7_0_0_A_asdO_000_D000_I_5p5_M1", "7_0_0_A_asdO_000_D000_I_9_M1", "7_0_0_A_asdO_000_D000_I_9_M1_rep0",
            "7_0_0_A_asdO_000_D000_I_12_M1", "7_0_TasdA_0_asdO_000_D000_0_ML_M2", "7_0_TasdA_0_asdO_000_D000_0_ML_M3",
            "7_D_Tasd_P_asdD_000_D000_0_4p5_M1", "7_D_Tasd_P_asdD_000_D000_0_5p5_M1", "7_D_Tasd_P_asdD_000_D000_0_9_M1",
            "7_D_Tasd_P_asdD_000_D000_0_12_M1", "7_D_Tasd_P_asdD_000_D000_0_24_M1", "7_D_Tasd_P_asdD_000_D000_I_5p5_M1",
            "7_D_Tasd_P_asdD_000_D000_I_9_M1", "7_D_Tasd_P_asdD_000_D000_I_12_M1", "7_D_Tasd_P_asdD_000_D000_I_24_M1" };

    @Test
    public void test() {
        List<String> samples = Arrays.stream(SAMPLES).collect(Collectors.toList());
        NewColumnCreator creator = NewColumnCreator.Type.SINGLE.create("7_0_0_A_asdO_000_D000_0_9_M1",
                "7_0_0_A_asdO_000_D000_I_5p5_M1", samples);
        assertThat(creator.getNewColumns(), contains("7_0_0_A_asdO_000_D000_0_9_M1,7_0_0_A_asdO_000_D000_I_5p5_M1"));
        creator = NewColumnCreator.Type.TIME1.create("7_0_0_A_asdO_000_D000_0_9_M1",
                "7_0_0_A_asdO_000_D000_I_5p5_M1", samples);
        assertThat(creator.getNewColumns(), contains("7_0_0_A_asdO_000_D000_0_3_M1,7_0_0_A_asdO_000_D000_I_5p5_M1",
                "7_0_0_A_asdO_000_D000_0_4p5_M1,7_0_0_A_asdO_000_D000_I_5p5_M1",
                "7_0_0_A_asdO_000_D000_0_5p5_M1,7_0_0_A_asdO_000_D000_I_5p5_M1",
                "7_0_0_A_asdO_000_D000_0_9_M1,7_0_0_A_asdO_000_D000_I_5p5_M1",
                "7_0_0_A_asdO_000_D000_0_12_M1,7_0_0_A_asdO_000_D000_I_5p5_M1",
                "7_0_0_A_asdO_000_D000_0_24_M1,7_0_0_A_asdO_000_D000_I_5p5_M1"));
        creator = NewColumnCreator.Type.TIMES.create("7_0_0_A_asdO_000_D000_0_9_M1",
                "7_0_0_A_asdO_000_D000_I_5p5_M1", samples);
        assertThat(creator.getNewColumns(), contains("7_0_0_A_asdO_000_D000_0_5p5_M1,7_0_0_A_asdO_000_D000_I_5p5_M1",
                "7_0_0_A_asdO_000_D000_0_9_M1,7_0_0_A_asdO_000_D000_I_9_M1",
                "7_0_0_A_asdO_000_D000_0_12_M1,7_0_0_A_asdO_000_D000_I_12_M1"));
    }

}
