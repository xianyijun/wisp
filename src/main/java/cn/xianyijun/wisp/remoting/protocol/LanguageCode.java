package cn.xianyijun.wisp.remoting.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LanguageCode {
    JAVA((byte) 0),
    CPP((byte) 1),
    DOTNET((byte) 2),
    PYTHON((byte) 3),
    DELPHI((byte) 4),
    ERLANG((byte) 5),
    RUBY((byte) 6),
    OTHER((byte) 7),
    HTTP((byte) 8),
    GO((byte) 9),
    PHP((byte) 10);

    private byte code;

    public static LanguageCode GgetLanguageByCode(byte code) {
        for (LanguageCode languageCode : LanguageCode.values()) {
            if (languageCode.getCode() == code) {
                return languageCode;
            }
        }
        return null;
    }
}
