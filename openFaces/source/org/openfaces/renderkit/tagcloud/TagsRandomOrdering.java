/*
 * OpenFaces - JSF Component Library 3.0
 * Copyright (C) 2007-2012, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */

package org.openfaces.renderkit.tagcloud;

import org.openfaces.component.tagcloud.TagCloudItem;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

/**
 * @author : roman.nikolaienko
 */
public class TagsRandomOrdering extends AbstractTagsOrderingStrategy {
        
    @Override
    public void order(List<TagCloudItem> items) {
        Collections.shuffle(items,new SecureRandom());
    }
}
