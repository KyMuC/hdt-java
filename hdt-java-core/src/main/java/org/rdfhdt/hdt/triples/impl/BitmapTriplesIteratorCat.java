/**
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
 *   Dennis Diefenbach:         dennis.diefenbach@univ-st-etienne.fr
 */

package org.rdfhdt.hdt.triples.impl;

import org.rdfhdt.hdt.dictionary.DictionaryCat;
import org.rdfhdt.hdt.dictionary.impl.utilCat.SectionUtil;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.dictionary.impl.utilCat.CatMapping;
import org.rdfhdt.hdt.dictionary.impl.FourSectionDictionaryCat;
import org.rdfhdt.hdt.exceptions.NotImplementedException;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleIDComparator;
import org.rdfhdt.hdt.triples.Triples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BitmapTriplesIteratorCat implements IteratorTripleID {

    int count = 1;

    Triples hdt1;
    Triples hdt2;
    Iterator<TripleID> list;
    DictionaryCat dictionaryCat;
    TripleIDComparator tripleIDComparator = new TripleIDComparator(TripleComponentOrder.SPO);

    public BitmapTriplesIteratorCat(Triples hdt1, Triples hdt2, DictionaryCat dictionaryCat){

        this.dictionaryCat = dictionaryCat;
        this.hdt1 = hdt1;
        this.hdt2 = hdt2;

        if(this.hdt1.getNumberOfElements() == 0 && this.hdt2.getNumberOfElements() == 0 ) {
            list = new ArrayList<TripleID>().listIterator();
        }
        else {
            list = getTripleID(1).listIterator();
            count++;
        }


    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public TripleID previous() {
        return null;
    }

    @Override
    public void goToStart() {

    }

    @Override
    public boolean canGoTo() {
        return false;
    }

    @Override
    public void goTo(long pos) {

    }

    @Override
    public long estimatedNumResults() {
        return hdt1.searchAll().estimatedNumResults()+hdt2.searchAll().estimatedNumResults();
    }

    @Override
    public ResultEstimationType numResultEstimation() {
        return null;
    }

    @Override
    public TripleComponentOrder getOrder() {
        return null;
    }

    @Override
    public long getLastTriplePosition() {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasNext() {
        if (count<dictionaryCat.getMappingS().size()){
            return true;
        } else {
			return list.hasNext();
        }
    }

    @Override
    public TripleID next() {
        if (!list.hasNext()) {
            list = getTripleID(count).listIterator();
            count++;
        }
        return list.next();
    }

    @Override
    public void remove() {

    }

    private List<TripleID> getTripleID(int count){
        Set<TripleID> set = new HashSet<>();
        List<Long> mapping = dictionaryCat.getMappingS().getMapping(count);
        List<Integer> mappingType = dictionaryCat.getMappingS().getType(count);

        for (int i = 0; i<mapping.size(); i++) {
            if (mappingType.get(i) == 1) {
                IteratorTripleID it = hdt1.search(new TripleID(mapping.get(i), 0, 0));
                while (it.hasNext()) {
                    set.add(mapTriple(it.next(), 1));
                }
            }
            if (mappingType.get(i) == 2) {
                IteratorTripleID it = hdt2.search(new TripleID(mapping.get(i), 0, 0));
                while (it.hasNext()) {
                    set.add(mapTriple(it.next(), 2));
                }
            }
        }
        ArrayList<TripleID> triples = new ArrayList<>(set);
        triples.sort(tripleIDComparator);
        return triples;
    }

    public TripleID mapTriple(TripleID tripleID, int num){
        if (num == 1){
            long new_subject1 = mapIdSection(tripleID.getSubject(), dictionaryCat.getAllMappings().get(SectionUtil.SH1),dictionaryCat.getAllMappings().get(SectionUtil.S1));
            long new_predicate1 = mapIdPredicate(tripleID.getPredicate(), dictionaryCat.getAllMappings().get(SectionUtil.P1));
            long new_object1 = mapIdSection(tripleID.getObject(), dictionaryCat.getAllMappings().get(SectionUtil.SH1),dictionaryCat.getAllMappings().get(SectionUtil.O1));
            return new TripleID(new_subject1, new_predicate1, new_object1);
        } else {
            long new_subject2 = mapIdSection(tripleID.getSubject(), dictionaryCat.getAllMappings().get(SectionUtil.SH2),dictionaryCat.getAllMappings().get(SectionUtil.S2));
            long new_predicate2 = mapIdPredicate(tripleID.getPredicate(), dictionaryCat.getAllMappings().get(SectionUtil.P2));
            long new_object2 = mapIdSection(tripleID.getObject(), dictionaryCat.getAllMappings().get(SectionUtil.SH2),dictionaryCat.getAllMappings().get(SectionUtil.O2));
            return new TripleID(new_subject2, new_predicate2, new_object2);
        }
    }

    private long mapIdSection(long id, CatMapping catMappingShared, CatMapping catMapping){
        if (id <= catMappingShared.getSize()){
            return catMappingShared.getMapping(id-1);
        } else {
            if (catMapping.getType(id-catMappingShared.getSize()-1)==1){
                return catMapping.getMapping(id-catMappingShared.getSize()-1);
            } else {
                return catMapping.getMapping(id - catMappingShared.getSize()-1) + dictionaryCat.getNumShared();
            }
        }
    }

    private long mapIdPredicate(long id, CatMapping catMapping){
        return catMapping.getMapping(id-1);
    }


}
