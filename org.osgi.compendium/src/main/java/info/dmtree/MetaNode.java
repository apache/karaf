/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.dmtree;

/**
 * The MetaNode contains meta data as standardized by OMA DM but extends it
 * (without breaking the compatibility) to provide for better DMT data quality
 * in an environment where many software components manipulate this data.
 * <p>
 * The interface has several types of functions to describe the nodes in the
 * DMT. Some methods can be used to retrieve standard OMA DM metadata such as
 * access type, cardinality, default, etc., others are for data extensions such
 * as valid names and values. In some cases the standard behaviour has been
 * extended, for example it is possible to provide several valid MIME types, or
 * to differentiate between normal and automatic dynamic nodes.
 * <p>
 * Most methods in this interface receive no input, just return information
 * about some aspect of the node. However, there are two methods that behave
 * differently, {@link #isValidName} and {@link #isValidValue}. These validation
 * methods are given a potential node name or value (respectively), and can
 * decide whether it is valid for the given node. Passing the validation methods
 * is a necessary condition for a name or value to be used, but it is not
 * necessarily sufficient: the plugin may carry out more thorough (more
 * expensive) checks when the node is actually created or set.
 * <p>
 * If a <code>MetaNode</code> is available for a node, the DmtAdmin must use the
 * information provided by it to filter out invalid requests on that node.
 * However, not all methods on this interface are actually used for this
 * purpose, as many of them (e.g. {@link #getFormat} or {@link #getValidNames})
 * can be substituted with the validating methods. For example,
 * {@link #isValidValue} can be expected to check the format, minimum, maximum,
 * etc. of a given value, making it unnecessary for the DmtAdmin to call
 * {@link #getFormat()}, {@link #getMin()}, {@link #getMax()} etc. separately.
 * It is indicated in the description of each method if the DmtAdmin does not
 * enforce the constraints defined by it - such methods are only for external
 * use, for example in user interfaces.
 * <p>
 * Most of the methods of this class return <code>null</code> if a certain piece
 * of meta information is not defined for the node or providing this information
 * is not supported. Methods of this class do not throw exceptions.
 * 
 * @version $Revision: 5673 $
 */
public interface MetaNode {

    /**
     * Constant for the ADD access type. If {@link #can(int)} returns
     * <code>true</code> for this operation, this node can potentially be
     * added to its parent. Nodes with {@link #PERMANENT} or {@link #AUTOMATIC}
     * scope typically do not have this access type.
     */
    int CMD_ADD = 0;

    /**
     * Constant for the DELETE access type. If {@link #can(int)} returns
     * <code>true</code> for this operation, the node can potentially be
     * deleted.
     */
    int CMD_DELETE = 1;

    /**
     * Constant for the EXECUTE access type. If {@link #can(int)} returns
     * <code>true</code> for this operation, the node can potentially be
     * executed.
     */
    int CMD_EXECUTE = 2;

    /**
     * Constant for the REPLACE access type. If {@link #can(int)} returns
     * <code>true</code> for this operation, the value and other properties of
     * the node can potentially be modified.
     */
    int CMD_REPLACE = 3;

    /**
     * Constant for the GET access type. If {@link #can(int)} returns
     * <code>true</code> for this operation, the value, the list of child nodes
     * (in case of interior nodes) and the properties of the node can
     * potentially be retrieved.
     */
    int CMD_GET = 4;

    /**
     * Constant for representing a permanent node in the tree. This must be
     * returned by {@link #getScope} if the node cannot be added, deleted or
     * modified in any way through tree operations. Permanent nodes cannot have
     * non-permanent nodes as parents.
     */
    int PERMANENT = 0;

    /**
     * Constant for representing a dynamic node in the tree. This must be
     * returned by {@link #getScope} for all nodes that are not permanent and
     * are not created automatically by the management object.
     */
    int DYNAMIC = 1;

    /**
     * Constant for representing an automatic node in the tree. This must be
     * returned by {@link #getScope()} for all nodes that are created
     * automatically by the management object. Automatic nodes represent a
     * special case of dynamic nodes, so this scope should be mapped to
     * {@link #DYNAMIC} when used in an OMA DM context.
     * <p>
     * An automatic node is usually created instantly when its parent is
     * created, but it is also valid if it only appears later, triggered by some
     * other condition. The exact behaviour must be defined by the Management
     * Object.
     */
    int AUTOMATIC = 2;

    /**
     * Check whether the given operation is valid for this node. If no meta-data
     * is provided for a node, all operations are valid.
     * 
     * @param operation One of the <code>MetaNode.CMD_...</code> constants.
     * @return <code>false</code> if the operation is not valid for this node
     *         or the operation code is not one of the allowed constants
     */
    boolean can(int operation);

    /**
     * Check whether the node is a leaf node or an internal one.
     * 
     * @return <code>true</code> if the node is a leaf node
     */
    boolean isLeaf();

    /**
     * Return the scope of the node. Valid values are
     * {@link #PERMANENT MetaNode.PERMANENT}, {@link #DYNAMIC MetaNode.DYNAMIC}
     * and {@link #AUTOMATIC MetaNode.AUTOMATIC}. Note that a permanent node is
     * not the same as a node where the DELETE operation is not allowed.
     * Permanent nodes never can be deleted, whereas a non-deletable node can
     * disappear in a recursive DELETE operation issued on one of its parents.
     * If no meta-data is provided for a node, it can be assumed to be a dynamic
     * node.
     * 
     * @return {@link #PERMANENT} for permanent nodes, {@link #AUTOMATIC} for
     *         nodes that are automatically created, and {@link #DYNAMIC}
     *         otherwise
     */
    int getScope();

    /**
     * Get the explanation string associated with this node. Can be
     * <code>null</code> if no description is provided for this node.
     * 
     * @return node description string or <code>null</code> for no description
     */
    String getDescription();

    /**
     * Get the number of maximum occurrences of this type of nodes on the same
     * level in the DMT. Returns <code>Integer.MAX_VALUE</code> if there is no
     * upper limit. Note that if the occurrence is greater than 1 then this node
     * can not have siblings with different metadata. In other words, if
     * different types of nodes coexist on the same level, their occurrence can
     * not be greater than 1. If no meta-data is provided for a node, there is
     * no upper limit on the number of occurrences.
     * 
     * @return The maximum allowed occurrence of this node type
     */
    int getMaxOccurrence();

    /**
     * Check whether zero occurrence of this node is valid. If no meta-data is
     * returned for a node, zero occurrences are allowed.
     * 
     * @return <code>true</code> if zero occurrence of this node is valid
     */
    boolean isZeroOccurrenceAllowed();

    /**
     * Get the default value of this node if any.
     * 
     * @return The default value or <code>null</code> if not defined
     */
    DmtData getDefault();

    /**
     * Get the list of MIME types this node can hold. The first element of the
     * returned list must be the default MIME type.
     * <p>
     * All MIME types are considered valid if no meta-data is provided for a
     * node or if <code>null</code> is returned by this method. In this case
     * the default MIME type cannot be retrieved from the meta-data, but the
     * node may still have a default. This hidden default (if it exists) can be
     * utilized by passing <code>null</code> as the type parameter of
     * {@link DmtSession#setNodeType(String, String)} or
     * {@link DmtSession#createLeafNode(String, DmtData, String)}.
     * 
     * @return the list of allowed MIME types for this node, starting with the
     *         default MIME type, or <code>null</code> if all types are
     *         allowed
     */
    String[] getMimeTypes();

    /**
     * Get the maximum allowed value associated with a node of numeric format.
     * If no meta-data is provided for a node, there is no upper limit to its
     * value. This method is only meaningful if the node has integer or float
     * format. The returned limit has <code>double</code> type, as this can be
     * used to denote both integer and float limits with full precision. The
     * actual maximum should be the largest integer or float number that does
     * not exceed the returned value.
     * <p>
     * The information returned by this method is not checked by DmtAdmin, it
     * is only for external use, for example in user interfaces. DmtAdmin only
     * calls {@link #isValidValue} for checking the value, its behaviour should
     * be consistent with this method.
     * 
     * @return the allowed maximum, or <code>Double.MAX_VALUE</code> if there
     *         is no upper limit defined or the node's format is not integer or
     *         float
     */
    double getMax();

    /**
     * Get the minimum allowed value associated with a node of numeric format.
     * If no meta-data is provided for a node, there is no lower limit to its
     * value. This method is only meaningful if the node has integer or float
     * format. The returned limit has <code>double</code> type, as this can be
     * used to denote both integer and float limits with full precision. The
     * actual minimum should be the smallest integer or float number that is
     * larger than the returned value.
     * <p>
     * The information returned by this method is not checked by DmtAdmin, it
     * is only for external use, for example in user interfaces. DmtAdmin only
     * calls {@link #isValidValue} for checking the value, its behaviour should
     * be consistent with this method.
     * 
     * @return the allowed minimum, or <code>Double.MIN_VALUE</code> if there
     *         is no lower limit defined or the node's format is not integer or
     *         float
     */
    double getMin();

    /**
     * Return an array of DmtData objects if valid values are defined for the
     * node, or <code>null</code> otherwise. If no meta-data is provided for a
     * node, all values are considered valid.
     * <p>
     * The information returned by this method is not checked by DmtAdmin, it
     * is only for external use, for example in user interfaces. DmtAdmin only
     * calls {@link #isValidValue} for checking the value, its behaviour should
     * be consistent with this method.
     * 
     * @return the valid values for this node, or <code>null</code> if not
     *         defined
     */
    DmtData[] getValidValues();

    /**
     * Get the node's format, expressed in terms of type constants defined in
     * {@link DmtData}. If there are multiple formats allowed for the node then
     * the format constants are OR-ed. Interior nodes must have
     * {@link DmtData#FORMAT_NODE} format, and this code must not be returned
     * for leaf nodes. If no meta-data is provided for a node, all applicable
     * formats are considered valid (with the above constraints regarding
     * interior and leaf nodes).
     * <p>
     * Note that the 'format' term is a legacy from OMA DM, it is more customary
     * to think of this as 'type'.
     * <p>
     * The formats returned by this method are not checked by DmtAdmin, they
     * are only for external use, for example in user interfaces. DmtAdmin only
     * calls {@link #isValidValue} for checking the value, its behaviour should
     * be consistent with this method.
     * 
     * @return the allowed format(s) of the node
     */
    int getFormat();
    
    /**
     * Get the format names for any raw formats supported by the node.  This
     * method is only meaningful if the list of supported formats returned by
     * {@link #getFormat()} contains {@link DmtData#FORMAT_RAW_STRING} or
     * {@link DmtData#FORMAT_RAW_BINARY}: it specifies precisely which raw
     * format(s) are actually supported.  If the node cannot contain data in one
     * of the raw types, this method must return <code>null</code>.
     * <p>
     * The format names returned by this method are not checked by DmtAdmin,
     * they are only for external use, for example in user interfaces. DmtAdmin
     * only calls {@link #isValidValue} for checking the value, its behaviour
     * should be consistent with this method.
     * 
     * @return the allowed format name(s) of raw data stored by the node, or
     *         <code>null</code> if raw formats are not supported
     */
    String[] getRawFormatNames();

    /**
     * Checks whether the given value is valid for this node. This method can be
     * used to ensure that the value has the correct format and range, that it
     * is well formed, etc. This method should be consistent with the
     * constraints defined by the {@link #getFormat}, {@link #getValidValues},
     * {@link #getMin} and {@link #getMax} methods (if applicable), as the Dmt
     * Admin only calls this method for value validation.
     * <p>
     * This method may return <code>true</code> even if not all aspects of the
     * value have been checked, expensive operations (for example those that
     * require external resources) need not be performed here. The actual value
     * setting method may still indicate that the value is invalid.
     * 
     * @param value the value to check for validity
     * @return <code>false</code> if the specified value is found to be
     *         invalid for the node described by this meta-node,
     *         <code>true</code> otherwise
     */
    boolean isValidValue(DmtData value);

    /**
     * Return an array of Strings if valid names are defined for the node, or
     * <code>null</code> if no valid name list is defined or if this piece of
     * meta info is not supported. If no meta-data is provided for a node, all
     * names are considered valid.
     * <p>
     * The information returned by this method is not checked by DmtAdmin, it
     * is only for external use, for example in user interfaces. DmtAdmin only
     * calls {@link #isValidName} for checking the name, its behaviour should be
     * consistent with this method.
     * 
     * @return the valid values for this node name, or <code>null</code> if
     *         not defined
     */
    String[] getValidNames();

    /**
     * Checks whether the given name is a valid name for this node. This method
     * can be used for example to ensure that the node name is always one of a
     * predefined set of valid names, or that it matches a specific pattern.
     * This method should be consistent with the values returned by
     * {@link #getValidNames} (if any), the DmtAdmin only calls this method for
     * name validation.
     * <p>
     * This method may return <code>true</code> even if not all aspects of the
     * name have been checked, expensive operations (for example those that
     * require external resources) need not be performed here. The actual node
     * creation may still indicate that the node name is invalid.
     * 
     * @param name the node name to check for validity
     * @return <code>false</code> if the specified name is found to be invalid
     *         for the node described by this meta-node, <code>true</code>
     *         otherwise
     */
    boolean isValidName(String name);

    /**
     * Returns the list of extension property keys, if the provider of this
     * <code>MetaNode</code> provides proprietary extensions to node meta
     * data. The method returns <code>null</code> if the node doesn't provide
     * such extensions.
     * 
     * @return the array of supported extension property keys
     */
    String[] getExtensionPropertyKeys();

    /**
     * Returns the value for the specified extension property key. This method
     * only works if the provider of this <code>MetaNode</code> provides
     * proprietary extensions to node meta data.
     * 
     * @param key the key for the extension property
     * @return the value of the requested property, cannot be <code>null</code>
     * @throws IllegalArgumentException if the specified key is not supported by
     *         this <code>MetaNode</code>
     */
    Object getExtensionProperty(String key);
}
