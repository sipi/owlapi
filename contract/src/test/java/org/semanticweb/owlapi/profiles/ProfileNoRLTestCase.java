package org.semanticweb.owlapi.profiles;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.semanticweb.owlapi.api.test.TestFiles;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;

@RunWith(Parameterized.class)
public class ProfileNoRLTestCase extends ProfileBase {

    private final String premise;

    public ProfileNoRLTestCase(String premise) {
        this.premise = premise;
    }

    @Parameters
    public static List<String> getData() {
        return Arrays.asList(TestFiles.profileNORLTestCases);
    }

    @Test
    public void testNoRL() {
        test(premise.startsWith("<rdf:RDF") ? new RDFXMLDocumentFormat() : new FunctionalSyntaxDocumentFormat(),
            premise, true, true, false, true);
    }
}
