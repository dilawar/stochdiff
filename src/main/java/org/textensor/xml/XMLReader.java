package org.textensor.xml;


import java.util.ArrayList;
import java.util.StringTokenizer;

import org.textensor.report.E;
import org.textensor.stochdiff.inter.BodyValued;


public class XMLReader {

    ReflectionInstantiator instantiator;
    int sourceLength;
    int nerror;

    double progressFraction;


    public XMLReader(ReflectionInstantiator insta) {
        instantiator = insta;
    }




    public void err(String s) {
        System.out.println(s);
    }


    public Object readObject(String s) {
        return readFromString(s);
    }


    public Object read(String s) {
        return readFromString(s);
    }



    public Object readFromString(String s) {
        s = XMLChecker.deGarbage(s);

        if (s == null) {
            return null;
        }

        progressFraction = 0.;

        nerror = 0;
        sourceLength = (new StringTokenizer(s, "\n")).countTokens();



        XMLTokenizer tkz = new XMLTokenizer(s);

        XMLToken xmlt = tkz.nextToken();
        while (xmlt.isIntro() || xmlt.isComment()) {

            if (xmlt.isComment()) {
                E.info("reading comment " + xmlt);
            }


            xmlt = tkz.nextToken();
        }



        XMLHolder xmlHolder = new XMLHolder();

        readFieldIntoParent(tkz, xmlHolder, xmlt);

        return xmlHolder.getContent();
    }



    class XMLHolder {

        Object content;


        public void setContent(Object obj) {
            content = obj;
        }


        public Object getContent() {
            return content;
        }
    }



    public XMLToken readToken(XMLTokenizer tkz) {
        XMLToken xmlt = tkz.nextToken();

        int lno = tkz.lineno();
        if (nerror > 4) {
            err("too many errors - aborting parse at line " + lno);

        }

        return xmlt;
    }



    public void readFieldIntoParent(XMLTokenizer tkz, Object parent, XMLToken start) {



        // read the child object that is known to the parent as item.name
        // if the parent is a vector, the object is added as a new element;
        // if the parent is a string, the xml is just apended;
        // otherwise the field is set.


        if (!start.isOpen()) {
            nerror++;
            err("ERROR - read object start item was not an open tag " + start);
            return;
        }


        Object child = null;

        if (parent instanceof String || parent instanceof StringBuffer) {
            child = new StringBuffer();
            ((StringBuffer)child).append(start.getOpenTagString());

        } else {

            // attributes may contain the class - the instantiator processes
            // all the attributes here
            Attribute[] atts = start.getAttributes();
            child = instantiator.getChildObject(parent, start.getName(), atts);
            if (child != null) {
                instantiator.applyAttributes(child, atts);
            }


            if (child == null) {
                child = new ArrayList();

            } else if (child instanceof String) {
                // in this case, set its length to 0. Subsequent parts of the
                // string will get appended to the current value, so want to
                // keep track of the fact that it is a string, without keeping
                // the default that may have come from above;
                child = new StringBuffer();


            } else if (child.getClass().isArray()) {
                // make it an array list for the time being, then
                // give the list to the instantiator to make into the right sort of
                // array;
                child = new ArrayList();
            }


            if (start.isClose()) {
                // the tag was both an open and a close tag, so now that we've
                // processed the attributes, we're done;


            } else {
                // read on and fill in fields until we get a closing tag which
                // matches the start tag
                // the fields will be inserted in target;

                XMLToken next = readToken(tkz);

                while (true) {
                    if (next.isNone()) {
                        // should mean EOF, but could also be an error
                        // return whatever;
                        break;


                    } else if (next.isOpen()) {
                        // open tags could mean anything - elementary field, array,
                        // or object, but in any case, pass them back to this method;
                        readFieldIntoParent(tkz, child, next);


                    } else if (next.isClose()) {
                        if (next.closes(start)) {
                            // fine - close item

                            if (parent instanceof String || parent instanceof StringBuffer) {
                                ((StringBuffer)child).append(next.getCloseTagString());
                            }


                        } else {
                            nerror++;
                            E.error(" non-matching close item \n" + "start Item was: \n"
                                         + start.toString() + "\n" + "but close was: \n" + next.toString() + "\n");
                        }

                        // stop anyway - either its the right close item, or
                        // the wrong one but lets cary on and see what happens;
                        break;


                    } else if (next.isString()) {
                        // this occurs if we're just reading a simple string
                        // field into the parent, or if we're in an array of strings;
                        // first case obj is defined, so reset it;
                        // second case put it in the vector;


                        if (child instanceof ArrayList) {
                            E.error("attempted to read string into array list?  - ignored" + next.svalue);

                            // ((ArrayList)child).add(next.svalue);


                        } else if (child instanceof StringBuffer) {
                            //   E.deprecate("xml reader - string added to string buffer " + next.svalue);

                            StringBuffer sbo = (StringBuffer)child;
                            String ssf = sbo.toString();
                            if (ssf.endsWith(">") || next.svalue.startsWith("<") || ssf.length() == 0) {
                                sbo.append(next.svalue);

                            } else {
                                sbo.append(" ");
                                sbo.append(next.svalue);
                            }


                        } else {
                            if (child instanceof String && ((String)child).length() > 0) {
                                child = child + " " + next.svalue;

                                //    E.deprecate("appended string to string " + next.svalue);

                            } else if (child == null) {
                                child = next.svalue;

                            }  else if (child instanceof Double && ((Double)child).doubleValue() == 0.0) {
                                child = new Double(next.svalue);

                                //          E.info("resetting dbl field " + parent + " " + start.getName() + " " + next.svalue);

                            } else if (child instanceof Integer && ((Integer)child).intValue() == 0) {
                                child = new Integer(next.svalue);

                            } else if (child instanceof BodyValued) {
                                ((BodyValued)child).setBodyValue(next.svalue);

                            } else {
                                instantiator.appendContent(child, next.svalue);

                            }
                        }


                    } else if (next.isNumber()) {
                        E.error("XMLReader sjhould never return numbers....!!!! but " + "just got " + next);
                    }
                    next = readToken(tkz);
                }
            }

            // presumably got a close object, and have done one of:
            // a) filled the parameters of obj;
            // b) replaced obj with a new object of the same type
            // c) filled the vector with strings, doubles or objects;



            if (child instanceof StringBuffer) {
                child = ((StringBuffer)child).toString();
            }

            /*
                     if (child instanceof ReReferencable) {
                        ((ReReferencable)child).reReference();
                     }
            */

            if (parent instanceof StringBuffer) {
                StringBuffer psb = (StringBuffer)parent;
                psb.append(child);
                psb.append("\n");


            } else if (parent instanceof XMLHolder) {
                ((XMLHolder)parent).setContent(child);

            } else if (parent instanceof ArrayList) {
                setListChild(parent, child);

            } else {
                instantiator.setField(parent, start.getName(), child);
            }
        }
    }



    @SuppressWarnings( {"unchecked"})
    private void setListChild(Object parent, Object child) {
        ((ArrayList)parent).add(child);
    }

}
