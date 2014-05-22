package com.google.appinventor.components.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.SemanticWebConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AlignmentUtil;
import com.google.appinventor.components.runtime.util.RdfUtil;
import com.google.appinventor.components.runtime.util.ViewUtil;

import android.app.Activity;
import android.util.Log;
import android.view.View;

/**
 * Linked Data Form provides a layout in which contained form elements will be
 * used to generate structured data. This form is used in conjunction with
 * the LinkedData component.
 * 
 * @see LinkedData
 * @see LinkedDataStore
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
@DesignerComponent(version = YaVersion.LINKED_DATA_FORM_COMPONENT_VERSION,
    description = "A layout that provides linked data enhancement of captured data.",
    category = ComponentCategory.LINKEDDATA)
@UsesLibraries(libraries = "xercesImpl.jar," + 
    "slf4j-android.jar," + "jena-iri.jar," + "jena-core.jar," +
    "jena-arq.jar")
@SimpleObject
public class LinkedDataForm extends AndroidViewComponent implements Component,
    ComponentContainer {

  private static final String LOG_TAG = LinkedDataForm.class.getSimpleName();
  /**
   * Stores a reference to the parent activity.
   */
  private final Activity context;

  /**
   * Linear layout used for arranging the contents of this form.
   */
  private final LinearLayout layout;

  private List<AndroidViewComponent> components;

  /**
   * String storing the URI of the concept used to type instances created with this form.
   */
  private String concept;

  /**
   * Stores the base URI used for naming new resources generated by this form.
   */
  private String baseUri;

  private String property;

  private String subject;

  private boolean inverse;

  /**
   * Creates a new linked data form in the specified container.
   * @param container
   */
  public LinkedDataForm(ComponentContainer container) {
    super(container);
    context = container.$context();
    layout = new LinearLayout(context,
        ComponentConstants.LAYOUT_ORIENTATION_VERTICAL,
        ComponentConstants.EMPTY_HV_ARRANGEMENT_WIDTH,
        ComponentConstants.EMPTY_HV_ARRANGEMENT_HEIGHT);
    AlignmentUtil alignmentSetter = new AlignmentUtil(layout);
    alignmentSetter.setHorizontalAlignment(ComponentConstants.HORIZONTAL_ALIGNMENT_DEFAULT);
    alignmentSetter.setVerticalAlignment(ComponentConstants.VERTICAL_ALIGNMENT_DEFAULT);
    components = new ArrayList<AndroidViewComponent>();
    concept = "";
    baseUri = "";
    property = "";
    Log.d(LOG_TAG, "Created linked data form");

    container.$add(this);
  }

  @Override
  public Activity $context() {
    return context;
  }

  @Override
  public Form $form() {
    return container.$form();
  }

  @Override
  public void $add(AndroidViewComponent component) {
    Log.d(LOG_TAG, "Added component to view layout");
    layout.add(component);
    components.add(component);
  }

  @Override
  public void setChildWidth(AndroidViewComponent component, int width) {
    ViewUtil.setChildWidthForVerticalLayout(component.getView(), width);
  }

  @Override
  public void setChildHeight(AndroidViewComponent component, int height) {
    ViewUtil.setChildHeightForVerticalLayout(component.getView(), height);
  }

  @Override
  public View getView() {
    Log.d(LOG_TAG, "Getting layout manager");
    return layout.getLayoutManager();
  }

  /**
   * Sets the concept URI to type objects encoded by this form.
   * @param uri
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CONCEPT_URI,
      defaultValue = "")
  @SimpleProperty
  public void ObjectType(String uri) {
    concept = uri;
  }

  /**
   * Returns the concept URI for this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "<p>When the contents of this form are turned into an "
          + "object description, the Uniform Resource Identifier (URI) "
          + "supplied for Object Type is used to identify the type of the "
          + "object.</p><p>For example, setting Object Type to "
          + "<code>http://xmlns.com/foaf/0.1/Person</code> (foaf:Person) will "
          + "identify the object on the web as a description of a person. This"
          + "allows other tools that understand foaf:Person to reuse data "
          + "generated by the form.</p>")
  public String ObjectType() {
    return concept;
  }

  /**
   * Sets the Base URI used for generating new subject identifiers
   * @param uri URI ending in # or /
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BASEURI_AUTOGEN,
      defaultValue = SemanticWebConstants.DEFAULT_BASE_URI)
  @SimpleProperty
  public void FormID(String uri) {
    baseUri = uri;
  }

  /**
   * Gets the Base URI of this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "<p>FormID is an autogenerated identifier for a form. It "
          + "is used in the generation of identifiers of object descriptions "
          + "created by this form when <code>AddDataFromLinkedDataForm</code> "
          + "is called on a <code>LinkedData</code> component. Setting "
          + "multiple forms to use the same FormID will allow them to change "
          + "the same set of objects.</p>")
  public String FormID() {
    return baseUri;
  }

  /**
   * Sets the property URI to link a parent form to this form.
   * @param uri 
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_PROPERTY_URI,
      defaultValue = "")
  @SimpleProperty
  public void PropertyURI(String uri) {
    property = uri;
  }

  /**
   * Gets the Property URI for linking a parent form to this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "If the form is placed within another Linked Data Form, "
          + "the Property URI specifies the relationship between the object "
          + "described by the outer form and the object described by the "
          + "inner form. For example, there may be an outer form of type "
          + "<a href='http://xmlns.com/foaf/spec/#term_Person' "
          + "target='_new'>foaf:Person</a> with an inner form of type "
          + "<a href='http://www.w3.org/TR/vcard-rdf/#d4e1126' "
          + "target='_new'>vcard:Address</a>. One could specify that the "
          + "Property URI on the Address form is vcard:hasAddress, indicating "
          + "that the Person described in the outer form has the address "
          + "described by the inner form. If the form has no outer form, then "
          + "the Property URI is ignored.")
  public String PropertyURI() {
    return property;
  }

  /**
   * Sets a Subject URI this form describes.
   * @param uri
   */
  @SimpleProperty
  public void Subject(String uri) {
    subject = uri;
  }

  /**
   * Gets the Subject URI for this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "<p>By default, the use of a form results in a new "
          + "description of an object. Setting the Subject property to a "
          + "specific Uniform Resource Identifier (URI) will cause the form to "
          + "write its description out as if it were talking about the "
          + "existing resource rather than a new resource. This is useful for "
          + "building an application for editing existing structured "
          + "content.</p>")
  public String Subject() {
    return subject;
  }

  /**
   * Sets if this form's property should be made the subject of a triple and its container the object.
   * @param inverse
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty
  public void InverseProperty(boolean inverse) {
    this.inverse = inverse;
  }

  /**
   * Gets whether or not this form represents an inverse property.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "<p>Inverse Property specifies whether the relationship "
          + "between the thing described by an outer form and the thing "
          + "described by an inner form should be reversed.</p><p>For example, "
          + "an application for movies may want to provide a method for "
          + "attributing a new movie to an existing director. However, "
          + "Schema.org's director property goes from a creative work to a "
          + "person. Using InverseProperty, one can start with a person and "
          + "attribute a new creative work (movie) to that person by choosing "
          + "the schema:director property and then setting InverseProperty to "
          + "True.</p>")
  public boolean InverseProperty() {
    return inverse;
  }

  @Override
  public Iterator<AndroidViewComponent> iterator() {
    Log.v(LOG_TAG, "Getting iterator for Linked Data Form. size = "+components.size());
    return components.iterator();
  }

  /**
   * Returns a URI for the form either by examining its Subject property or
   * generated from its contents.
   * @return The empty string if no valid URL can be constructed for the form,
   * or a valid URI that can be used to represent the contents of the form.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Provides a URI for the form even if SubjectURI is not set.")
  public String GenerateSubjectURI() {
    if(Subject().length() == 0) {
      String subj = RdfUtil.generateSubjectForForm(this);
      if(subj == null) {
        return "";
      } else {
        return subj;
      }
    } else {
      return Subject();
    }
  }
}
