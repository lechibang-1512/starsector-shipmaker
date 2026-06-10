package shipeditor.utility.components.rendering;

import lombok.Getter;
import lombok.Setter;

import javax.swing.tree.DefaultMutableTreeNode;

@Getter @Setter
public class CustomTreeNode extends DefaultMutableTreeNode {

    private String firstLineTip;

    private String secondLineTip;

    private String thirdLineTip;

    @SuppressWarnings("ParameterHidesMemberVariable")
    public CustomTreeNode(Object userObject) {
        super(userObject);
    }

    @Override
    public Object clone() {
        CustomTreeNode cloned = (CustomTreeNode) super.clone();
        cloned.firstLineTip = this.firstLineTip;
        cloned.secondLineTip = this.secondLineTip;
        cloned.thirdLineTip = this.thirdLineTip;
        return cloned;
    }

}
