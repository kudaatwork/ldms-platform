package projectlx.co.zw.shared_library.utils.i18n.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import projectlx.co.zw.shared_library.utils.i18n.api.MessageService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageSource messageSource;

    @Override
    public String getMessage(String code, String[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }
}
