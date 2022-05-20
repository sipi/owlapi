package org.semanticweb.owlapi.atomicdecomposition;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.modularity.ModuleType;
import org.semanticweb.owlapi.modularity.SyntacticLocalityModuleExtractor;

public class ModuleAnnotationsTestCase extends TestBase {

    private static final String input = "annotationproperty(term_replaced_by)\n"
        + "A term_replaced_by (annotationproperty) B\n" + "B owl:deprecated true";
    private OWLAnnotationProperty a = df.getOWLAnnotationProperty("urn:test:a");
    private OWLAnnotationProperty b = df.getOWLAnnotationProperty("urn:test:b");
    private OWLAnnotationProperty replaced =
        df.getOWLAnnotationProperty("urn:test:term_replaced_by");
    OWLDeclarationAxiom dA = df.getOWLDeclarationAxiom(a);
    OWLDeclarationAxiom dR = df.getOWLDeclarationAxiom(replaced);
    OWLDeclarationAxiom dB = df.getOWLDeclarationAxiom(b);
    OWLAnnotationAssertionAxiom ax =
        df.getOWLAnnotationAssertionAxiom(replaced, a.getIRI(), b.getIRI());
    Set<OWLEntity> e = new HashSet<>(Arrays.asList(a));

    @Test
    public void shouldNotAddAnnotations() throws OWLOntologyCreationException {
        Set<OWLAxiom> expected = new HashSet<OWLAxiom>();
        OWLOntology o = m.createOntology(df.getIRI("urn:test:noanns"));
        o.getOntologyConfigurator().withSkipModuleAnnotations(true);
        o.add(dR, dA, dB, ax);
        Set<OWLAxiom> module =
            new SyntacticLocalityModuleExtractor(m, o, ModuleType.STAR).extract(e);
        assertEquals(expected, module);
    }

    @Test
    public void shouldAddAnnotations() throws OWLOntologyCreationException {
        Set<OWLAxiom> expected = new HashSet<OWLAxiom>();
        expected.add(ax);
        expected.add(dA);
        OWLOntology o = m.createOntology(df.getIRI("urn:test:anns"));
        o.add(dR, dA, dB, ax);
        Set<OWLAxiom> module =
            new SyntacticLocalityModuleExtractor(m, o, ModuleType.STAR).extract(e);
        assertEquals(expected, module);
    }
}
