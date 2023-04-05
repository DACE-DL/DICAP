package org.example;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Alignment {


    static class OntologyNode {
        public static final List<String> classes = new ArrayList<>();
        public static final List<String> properties = new ArrayList<>();

        public String getTag() {
            return tag;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "OntologyNode{" +
                    "tag='" + tag + '\'' +
                    ", name='" + name + '\'' +
                    ", attributes=" + attributes +
                    ", children=" + children +
                    '}';
        }

        private String tag, name;


        private Map<String, String> attributes = new HashMap<>();
        private final List<OntologyNode> children = new ArrayList<>();

        private OntologyNode() {
        }


        public static OntologyNode fromXMLNode(Node node) {
            OntologyNode ontologyNode = new OntologyNode();
            ontologyNode.tag = node.getNodeName();

            Map<String, String> attributeMap = new HashMap<>();
            NamedNodeMap attributes = node.getAttributes();

            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    attributeMap.put(attributes.item(i).getNodeName(), attributes.item(i).getNodeValue());
                }
            }

            ontologyNode.attributes = attributeMap;
            if (ontologyNode.attributes.containsKey("rdf:about")) {
                ontologyNode.name = ontologyNode.attributes.get("rdf:about");
            }

            return ontologyNode;
        }

        public static OntologyNode fromTxt(String name, String tag) {
            OntologyNode ontologyNode = new OntologyNode();
            ontologyNode.name = name;
            ontologyNode.tag = tag;
            return ontologyNode;
        }




        public String toMergedForm() {
            String[] split = tag.split(":");
            String name = this.name == null ? "" : "_" + this.name;
            StringBuilder base = new StringBuilder(split[split.length - 1] + name);
            for (OntologyNode child : children) {
                base.append("+").append(child.toMergedForm());
            }

            return base.toString().replace("#", "_");
        }

    }

    private OntologyNode element1, element2;
    private String relation;
    private float measure;

    Alignment() {
    }


    public OntologyNode getElement1() {
        return element1;
    }

    public OntologyNode getElement2() {
        return element2;
    }

    public static Set<Alignment> readAlignments(String path) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new File(path));
        doc.getDocumentElement().normalize();
        Node item = doc.getDocumentElement().getChildNodes().item(1);
        Set<Alignment> alignments = new HashSet<>();
        for (int i = 0; i < item.getChildNodes().getLength(); i++) {
            if (!item.getChildNodes().item(i).getNodeName().equals("map")) continue;
            Node item1 = item.getChildNodes().item(i).getChildNodes().item(1);

            Alignment alignment = new Alignment();
            for (int i1 = 0; i1 < item1.getChildNodes().getLength(); i1++) {
                Node item2 = item1.getChildNodes().item(i1);
                switch (item2.getNodeName()) {
                    case "entity1" -> alignment.element1 = parseNode(item2.getChildNodes().item(1));
                    case "entity2" -> alignment.element2 = parseNode(item2.getChildNodes().item(1));
                    case "relation" -> alignment.relation = item2.getChildNodes().item(0).getNodeValue();
                    case "measure" ->
                            alignment.measure = Float.parseFloat(item2.getChildNodes().item(0).getNodeValue());
                }

            }
            alignments.add(alignment);

        }

        return alignments;
    }

    @Override
    public String toString() {
        return "Alignment{" +
                "element1=" + element1 +
                ", element2=" + element2 +
                ", relation='" + relation + '\'' +
                ", measure=" + measure +
                '}';
    }

    public static Set<Alignment> readAlignmentsTxt(Path path)  {
        Set<Alignment> alignments = new HashSet<>();
        try (Scanner scanner = new Scanner(path)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] split = line.split("\\|");
                Alignment alignment = new Alignment();
                alignment.element1 = OntologyNode.fromTxt(split[0], split[4]);
                alignment.element2 = OntologyNode.fromTxt(split[1], split[4]);
                alignment.relation = split[2];
                alignment.measure = Float.parseFloat(split[3]);
                alignments.add(alignment);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return alignments;

    }


    public static OntologyNode parseNode(Node node) {

        OntologyNode ontologyNode = OntologyNode.fromXMLNode(node);

        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node item = node.getChildNodes().item(i);
            if (item.getNodeName().equals("#text")) continue;
            OntologyNode node1 = parseNode(item);
            ontologyNode.children.add(node1);
        }

        return ontologyNode;
    }


}
