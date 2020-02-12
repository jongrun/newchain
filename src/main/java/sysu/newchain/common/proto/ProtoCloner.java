package sysu.newchain.common.proto;

import com.google.protobuf.MessageOrBuilder;

/**
 * @Description TODO
 * @author jongliao
 * @date 2020年2月1日 下午9:07:24
 */
public interface ProtoCloner<O, P extends MessageOrBuilder> {
	
	public O toObject(P p);
	
	public P toProto(O o);
	
}
