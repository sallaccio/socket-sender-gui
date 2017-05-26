package utility;

import javax.swing.*;

import java.util.*;

@SuppressWarnings("serial")
public class SortedListOfActions extends AbstractListModel {

	SortedSet<ActionListElement> model;

	public SortedListOfActions() {
		model = new TreeSet<ActionListElement>();
	}

	public int getSize() {
		return model.size();
	}

	public String getElementAt(int index) {
		return ((ActionListElement)model.toArray()[index]).Name;
	}
	
	public ActionListElement getActionListElementAt(int index) {
		return (ActionListElement)model.toArray()[index];
	}

	public int indexOf(String elementName)
	{
		int index = -1;
		for (int i = 0; i < model.size(); i++)
		{
			if (elementName.equalsIgnoreCase(((ActionListElement)model.toArray()[i]).Name))
				return i;
		}
		return index;
	}
	
	public void add(String elementName, ActionType elementType)
	{
		add(elementName, elementType, true);
	}
	
	public void add(String elementName, ActionType elementType, boolean isCorrect)
	{
		add(new ActionListElement(elementName, elementType, isCorrect));
	}
	
	public void add(ActionListElement element) {
		if (model.add(element)) {
			fireContentsChanged(this, 0, getSize());
		}
	}
	
	public void addAll(ActionListElement[] elements) {
		Collection<ActionListElement> c = Arrays.asList(elements);
		model.addAll(c);
		fireContentsChanged(this, 0, getSize());
	}

	public void clear() {
		model.clear();
		fireContentsChanged(this, 0, getSize());
	}

	public boolean contains(String elementName) {
		Boolean found = false;
		for (int i = 0; i < this.getSize(); i++)
		{
			found = this.getElementAt(i).equalsIgnoreCase(elementName);
		}
		return found;
	}

	public boolean contains(ActionListElement element) {
		return contains(element.Name);
	}

	public ActionListElement firstElement() {
		return model.first();
	}

	public Iterator<ActionListElement> iterator() {
		return model.iterator();
	}

	public ActionListElement lastElement() {
		return model.last();
	}

	public boolean removeElement(String elementName) {
		boolean removed = false;
		for (int i = 0; i < this.getSize(); i++)
		{
			if (this.getElementAt(i).equalsIgnoreCase(elementName))
				removed = model.remove(this.getElementAt(i));
		}
		if (removed) {
			fireContentsChanged(this, 0, getSize());
		}
		return removed;
	}

	public boolean removeElement(ActionListElement element)
	{
		boolean removed = model.remove(element);
		if (removed) {
			fireContentsChanged(this, 0, getSize());
		}
		return removed;
	}

}