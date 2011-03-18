/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.util;

import drinkcounter.DrinkCounterService;
import drinkcounter.model.Drink;
import drinkcounter.model.Participant;
import drinkcounter.model.Party;
import java.io.OutputStream;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Toni
 */
public class PartyMarshaller {

    @Autowired
    private DrinkCounterService service;

    public void marshall(String partyId, OutputStream out){
        try {
            Party party = service.getParty(partyId);
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder b = f.newDocumentBuilder();
            Document d = b.newDocument();
            Node rootNode = d.createElement("party");
            d.appendChild(rootNode);
            Node idNode = d.createElement("id");
            idNode.setTextContent(party.getId());
            rootNode.appendChild(idNode);
            Node partyNameNode = d.createElement("name");
            partyNameNode.setTextContent(party.getName());
            rootNode.appendChild(partyNameNode);
            Node participantsNode = d.createElement("participants");
            rootNode.appendChild(participantsNode);
            for (Participant participant : service.listParticipants(partyId)) {
                participantsNode.appendChild(createParticipantNode(d, participant));
            }
            StreamResult streamResult = new StreamResult(out);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(new DOMSource(d), streamResult);
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

     private Node createParticipantNode(Document doc, Participant participant) {
        Node participantNode = doc.createElement("participant");
        
        List<Drink> drinks = service.getDrinks(participant.getId());
        participant.setDrinks(drinks);

        participantNode.appendChild(createTextContentElement("id", participant.getId(), doc));
        participantNode.appendChild(createTextContentElement("name", participant.getName(), doc));
        participantNode.appendChild(createTextContentElement("alcoholInPromilles", Float.toString(participant.getPromilles()), doc));
        participantNode.appendChild(createTextContentElement("weight", Float.toString(participant.getWeight()), doc));
        participantNode.appendChild(createTextContentElement("sex", participant.getSex().toString(), doc));
        participantNode.appendChild(createDrinksNode(doc, drinks));

        return participantNode;
    }

    private Node createDrinksNode(Document d, List<Drink> drinks){
        Node drinksNode = d.createElement("drinks");
        drinksNode.appendChild(createTextContentElement("count", Integer.toString(drinks.size()), d));
        for (Drink drink : drinks) {
            Node drinkNode = d.createElement("drink");
            DateTime dateTime = new DateTime(drink.getTimeStamp());
            drinkNode.appendChild(createTextContentElement("timestamp", dateTime.toString(), d));
            drinksNode.appendChild(drinkNode);
        }
        return drinksNode;
    }

    private Element createTextContentElement(String name, String content, Document document){
        Element element = document.createElement(name);
        element.setTextContent(content);
        return element;
    }
}
