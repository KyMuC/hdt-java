/*
 * File: $HeadURL: https://hdt-java.googlecode.com/svn/trunk/hdt-java/src/org/rdfhdt/hdt/dictionary/impl/section/HashDictionarySection.java $
 * Revision: $Rev: 191 $
 * Last modified: $Date: 2013-03-03 11:41:43 +0000 (dom, 03 mar 2013) $
 * Last modified by: $Author: mario.arias $
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */

package org.rdfhdt.hdt.dictionary.impl.section;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.rdfhdt.hdt.dictionary.TempDictionarySection;
import org.rdfhdt.hdt.options.HDTOptions;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.util.LiteralsUtils;
import org.rdfhdt.hdt.util.string.ByteStringUtil;
import org.rdfhdt.hdt.util.string.CharSequenceComparator;
import org.rdfhdt.hdt.util.string.CharSequenceCustomComparator;
import org.rdfhdt.hdt.util.string.CompactString;

/**
 * @author mario.arias
 *
 */
public class HashDictionarySection implements TempDictionarySection {
	public static final int TYPE_INDEX = 1;

	private HashMap<CharSequence, Long> map;
	private List<CharSequence> list;
	private int size;
	public boolean sorted;
	final boolean isCustom;
	private final Map<CharSequence,Long> literalsCounts = new HashMap<>();
	/**
	 *
	 */
	public HashDictionarySection(boolean isCustom) {
		this.isCustom = isCustom;
		map = new HashMap<>();
		list = new ArrayList<>();
		size=0;
	}
	public HashDictionarySection() {
		this(false);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public long locate(CharSequence s) {
		Long val = map.get(ByteStringUtil.asByteString(s));
		if(val==null) {
			return 0;
		}
		return val;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(long pos) {
		if(pos<=0) {
			return null;
		}
		return list.get((int) (pos-1));
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		return size;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return list.size();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getEntries()
	 */
	@Override
	public Iterator<? extends CharSequence> getSortedEntries() {
		if(!sorted) {
			return null;
		}
		return list.iterator();
	}

	@Override
	public Iterator<? extends CharSequence> getEntries() {
		return list.iterator();
	}

	@Override
	public long add(CharSequence entry) {
		CharSequence compact = new CompactString(entry);
		return map.computeIfAbsent(compact, key -> {
			// Not found, insert new
			list.add(compact);
			size+=compact.length();
			sorted = false;

			// custom for subsection literals ..
			if (isCustom) {
				CharSequence type = LiteralsUtils.getType(compact);
				// check if the entry doesn't already exist
				literalsCounts.compute(type, (key2, count) -> count == null ? 1L : count + 1L);
			}
			return (long) list.size();
		});
	}

	@Override
	public void remove(CharSequence seq) {
		map.remove(seq);
		sorted = false;
	}

	@Override
	public void sort() {
		// Update list.
		list = new ArrayList<>(map.size());
		list.addAll(map.keySet());

		// Sort list
		if (isCustom) {
			list.sort(new CharSequenceCustomComparator());
		} else {
			list.sort(new CharSequenceComparator());
		}

		// Update map indexes
		for (long i = 1; i <= getNumberOfElements(); i++) {
			map.put(extract(i), i);
		}

		sorted = true;
	}

	@Override
	public boolean isSorted() {
		return sorted;
	}

	@Override
	public void clear() {
		list.clear();
		map.clear();
		size = 0;
		sorted = false; //because if sorted won't be anymore
	}

	@Override
	public void close() throws IOException {
		map = null;
		list = null;
	}

	@Override
	public Map<CharSequence, Long> getLiteralsCounts() {
		return literalsCounts;
	}
}
