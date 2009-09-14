package org.apache.felix.sigil.ui.eclipse.ui.util;

public class UIHelper
{
    public static <T> IElementDescriptor<T> getDefaultElementDescriptor() {
        return new IElementDescriptor<T>()
        {
            public String getLabel( T element )
            {
                return element == null ? "null" : element.toString();
            }


            public String getName( T element )
            {
                return getLabel( element );
            }
        };
    }
    
    public static <T> IFilter<T> getDefaultFilter() {
        return new IFilter<T> () {
            public boolean select( T element )
            {
                return true;
            }
        };
    }
}
