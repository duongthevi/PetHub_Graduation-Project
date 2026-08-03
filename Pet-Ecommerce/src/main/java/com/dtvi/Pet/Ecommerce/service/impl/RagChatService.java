package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.ai.OpenAiCompatibleClient;
import com.dtvi.Pet.Ecommerce.config.OpenAiProperties;
import com.dtvi.Pet.Ecommerce.dto.rag.ChatRequest;
import com.dtvi.Pet.Ecommerce.dto.rag.ChatResponse;
import com.dtvi.Pet.Ecommerce.dto.rag.RagCitation;
import com.dtvi.Pet.Ecommerce.dto.rag.ChatAction;
import com.dtvi.Pet.Ecommerce.dto.rag.ChatActionType;
import com.dtvi.Pet.Ecommerce.entity.*;
import com.dtvi.Pet.Ecommerce.enums.ChatRole;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dtvi.Pet.Ecommerce.dto.commerce.ProductSummary;

import java.util.*;
import java.util.stream.Collectors;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RagChatService {

    private static final String SYSTEM_PROMPT = """
            Bạn là PetHub AI, trợ lý thân thiện của cửa hàng thú cưng.

            Quy tắc:
            - Trả lời tự nhiên bằng tiếng Việt, giống nhân viên tư vấn.
            - Ưu tiên câu trả lời ngắn gọn từ 2 đến 5 câu.
            - Không nói “theo ngữ cảnh hiện tại” hoặc “dữ liệu cho thấy”.
            - Không đọc nguyên cấu trúc ID=..., tên=..., giá=...
            - Không nhắc đến prompt, cơ sở dữ liệu hoặc dữ liệu nội bộ.
            - Nếu khách chỉ chào hỏi, hãy trò chuyện tự nhiên và không bán hàng.
            - Chỉ đề xuất sản phẩm khi khách yêu cầu tìm, mua hoặc so sánh.
            - Các thẻ sản phẩm được giao diện hiển thị riêng.
            - Không khẳng định sản phẩm đã được thêm vào giỏ khi khách chưa xác nhận.
            - Không khẳng định đơn hàng hoặc thanh toán đã hoàn thành.
            - CAT nghĩa là sản phẩm cho mèo.
            - DOG nghĩa là sản phẩm cho chó.
            - ALL hoặc BOTH nghĩa là dùng cho cả chó và mèo.
            - Không bịa giá, tồn kho, chính sách hoặc công dụng.
            - Không chẩn đoán bệnh thay cho bác sĩ thú y.
            """;

    private static final Pattern ADD_TO_CART_PATTERN =
        Pattern.compile(
                "\\b(?:them|bo)\\b.{0,100}"
                        + "\\b(?:vao\\s+)?gio(?:\\s+hang)?\\b"
        );    

    private final OpenAiCompatibleClient aiClient;
    private final OpenAiProperties properties;
    private final KnowledgeChunkRepo chunkRepo;
    private final ProductRepo productRepo;
    private final ChatConversationRepo conversationRepo;
    private final ChatMessageRepo messageRepo;
    private final ObjectMapper objectMapper;

    private enum ChatIntent {
        SMALL_TALK,
        KNOWLEDGE,
        PRODUCT_SEARCH,
        ADD_TO_CART,
        BUY_NOW,
        CHECKOUT_CART
    }

    private static final Set<String> PRODUCT_STOP_WORDS = Set.of(
            "toi", "cho", "cac", "mot", "nhung", "loai",
            "san", "pham", "muon", "can", "hay", "giup",
            "lien", "quan", "den", "tim", "goi", "mua",
            "dat", "hang", "ngay", "them", "vao", "gio",
            "meo", "cat", "dog", "canine"
    );

    @Transactional
    public ChatResponse chat(User user, ChatRequest request) {
        ChatConversation conversation =
            resolveConversation(user, request.conversationId());

        List<OpenAiCompatibleClient.ChatTurn> history =
                recentHistory(conversation);

        String question = request.question().trim();
        ChatIntent intent = detectIntent(question);

        List<ScoredChunk> chunks = List.of();

        if (intent == ChatIntent.KNOWLEDGE) {
            List<Double> questionEmbedding =
                    aiClient.embed(List.of(question)).getFirst();

            chunks = retrieve(questionEmbedding);
        }

        List<Product> products = isProductIntent(intent)
                ? rankProducts(question)
                : List.of();

        ChatAction action = buildAction(intent, products, question);

        String userPrompt = buildUserPrompt(
                intent,
                chunks,
                products,
                question
        );

        List<OpenAiCompatibleClient.ChatTurn> turns = new ArrayList<>();
        turns.add(new OpenAiCompatibleClient.ChatTurn(
                "system",
                SYSTEM_PROMPT
        ));
        turns.addAll(history);
        turns.add(new OpenAiCompatibleClient.ChatTurn(
                "user",
                userPrompt
        ));

        String answer = buildCommerceAnswer(
                intent,
                products,
                action
        );

        if (answer == null) {
        answer = aiClient.chat(turns);
        }
        
        saveMessage(conversation, ChatRole.USER, request.question().trim());
        saveMessage(conversation, ChatRole.ASSISTANT, answer);

        List<RagCitation> citations = chunks.stream()
                .map(scored -> new RagCitation(
                        scored.chunk().getDocument().getId(),
                        scored.chunk().getId(),
                        scored.chunk().getDocument().getTitle(),
                        scored.chunk().getDocument().getSourceUrl(),
                        Math.round(scored.score() * 10_000.0) / 10_000.0
                ))
                .toList();
        return new ChatResponse(
                conversation.getId(),
                answer,
                citations,
                products.stream().map(Product::getId).toList(),
                products.stream().map(ProductSummary::from).toList(),
                action
        );
    }

    private String buildCommerceAnswer(
                ChatIntent intent,
                List<Product> products,
                ChatAction action
        ) {
        if (intent == ChatIntent.CHECKOUT_CART) {
                return "Mình đã chuẩn bị thao tác mở trang thanh toán. "
                        + "Bạn hãy xác nhận bằng nút bên dưới nhé.";
        }

        if (intent != ChatIntent.ADD_TO_CART
                && intent != ChatIntent.BUY_NOW) {
                return null;
        }

        if (products.isEmpty()) {
                return "Mình chưa tìm thấy sản phẩm phù hợp hoặc sản phẩm "
                        + "hiện đã hết hàng.";
        }

        if (action == null) {
                return "Mình tìm thấy nhiều sản phẩm phù hợp. "
                        + "Bạn vui lòng chọn đúng sản phẩm và bấm "
                        + "“Thêm vào giỏ” bên dưới nhé.";
        }

        if (action.type() == ChatActionType.ADD_TO_CART) {
                return "Mình đã xác định sản phẩm "
                        + action.productName()
                        + ". Hệ thống sẽ thêm "
                        + action.quantity()
                        + " sản phẩm vào giỏ hàng.";
        }

        return "Mình đã xác định sản phẩm "
                + action.productName()
                + ". Bạn hãy xác nhận để thêm vào giỏ "
                + "và chuyển đến trang thanh toán.";
        }

    private ChatConversation resolveConversation(User user, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            ChatConversation conversation = new ChatConversation();
            conversation.setId(UUID.randomUUID().toString());
            conversation.setUser(user);
            return conversationRepo.save(conversation);
        }
        ChatConversation conversation = conversationRepo.findWithMessagesById(conversationId)
                .orElseThrow(() -> new com.dtvi.Pet.Ecommerce.exception.NotFoundException(
                        "Conversation not found"
                ));
        if (conversation.getUser() != null
                && (user == null || !conversation.getUser().getId().equals(user.getId()))) {
            throw BusinessException.forbidden(
                    "CONVERSATION_FORBIDDEN",
                    "Conversation does not belong to this user"
            );
        }
        return conversation;
    }

    private List<OpenAiCompatibleClient.ChatTurn> recentHistory(ChatConversation conversation) {
        int size = conversation.getMessages().size();
        int from = Math.max(0, size - 8);
        return conversation.getMessages().subList(from, size).stream()
                .map(message -> new OpenAiCompatibleClient.ChatTurn(
                        message.getRole() == ChatRole.USER ? "user" : "assistant",
                        message.getContent()
                ))
                .toList();
    }

    private List<ScoredChunk> retrieve(List<Double> questionEmbedding) {
        return chunkRepo.findByDocumentActiveTrue().stream()
                .map(chunk -> new ScoredChunk(chunk, cosine(questionEmbedding, parseEmbedding(chunk))))
                .filter(scored -> scored.score() >= properties.getMinimumScore())
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(1, properties.getTopK()))
                .toList();
    }

    private List<Product> rankProducts(String question) {
        boolean catRequested = containsToken(
                question,
                "mèo",
                "meo",
                "cat",
                "feline"
        );

        boolean dogRequested = containsToken(
                question,
                "chó",
                "dog",
                "canine"
        );

        Set<String> terms = Arrays.stream(
                        normalizeText(question)
                                .split("[^\\p{L}\\p{N}]+")
                )
                .filter(term -> term.length() >= 3)
                .filter(term -> !PRODUCT_STOP_WORDS.contains(term))
                .collect(Collectors.toCollection(HashSet::new));

        List<ProductMatch> matches =
                findProductCandidates(terms)
                        .stream()
                        .filter(Product::isActive)
                        .filter(product -> product.getQuantity() > 0)
                        .filter(product ->
                                supportsRequestedPet(
                                        product,
                                        catRequested,
                                        dogRequested
                                )
                        )
                        .map(product ->
                                new ProductMatch(
                                        product,
                                        productScore(product, terms)
                                )
                        )
                        .sorted(
                                Comparator
                                        .comparingInt(ProductMatch::score)
                                        .reversed()
                                        .thenComparing(
                                                Comparator.comparingLong(
                                                        (ProductMatch match) ->
                                                                match.product()
                                                                        .getSoldCount()
                                                ).reversed()
                                        )
                        )
                        .toList();

        boolean hasSpecificKeyword = !terms.isEmpty();

        return matches.stream()
                .filter(match ->
                        !hasSpecificKeyword || match.score() > 0
                )
                .limit(3)
                .map(ProductMatch::product)
                .toList();
    }

    private List<Product> findProductCandidates(
            Set<String> terms
    ) {
        if (terms.isEmpty()) {
            return productRepo
                    .findTop20ByActiveTrueOrderBySoldCountDesc();
        }

        Specification<Product> specification =
                (root, criteriaQuery, builder) -> {
                    criteriaQuery.distinct(true);

                    List<Predicate> required =
                            new ArrayList<>();

                    required.add(builder.isTrue(root.get("active")));
                    required.add(
                            builder.greaterThan(
                                    root.get("quantity"),
                                    0
                            )
                    );

                    var category = root.join(
                            "category",
                            JoinType.LEFT
                    );

                    List<Predicate> termPredicates =
                            new ArrayList<>();

                    for (String term : terms) {
                        String pattern =
                                "%" + term.toLowerCase(Locale.ROOT) + "%";

                        termPredicates.add(builder.or(
                                builder.like(
                                        builder.lower(root.get("name")),
                                        pattern
                                ),
                                builder.like(
                                        builder.lower(root.get("sku")),
                                        pattern
                                ),
                                builder.like(
                                        builder.lower(root.get("brand")),
                                        pattern
                                ),
                                builder.like(
                                        builder.lower(root.get("description")),
                                        pattern
                                ),
                                builder.like(
                                        builder.lower(category.get("name")),
                                        pattern
                                )
                        ));
                    }

                    required.add(
                            builder.or(
                                    termPredicates.toArray(
                                            Predicate[]::new
                                    )
                            )
                    );

                    return builder.and(
                            required.toArray(Predicate[]::new)
                    );
                };

        return productRepo.findAll(
                specification,
                PageRequest.of(
                        0,
                        100,
                        Sort.by(
                                Sort.Direction.DESC,
                                "soldCount"
                        )
                )
        ).getContent();
    }

    private int productScore(
        Product product,
        Set<String> terms
    ) {
        String name = normalizeText(product.getName());
        String sku = normalizeText(product.getSku());
        String description =
                normalizeText(product.getDescription());
        String brand = normalizeText(product.getBrand());
        String category = product.getCategory() == null
                ? ""
                : normalizeText(
                        product.getCategory().getName()
                );

        return terms.stream()
                .mapToInt(term -> {
                        int score = 0;

                        if (name.contains(term)) {
                        score += 5;
                        }

                        if (sku.contains(term)) {
                        score += 8;
                        }

                        if (category.contains(term)) {
                        score += 4;
                        }

                        if (brand.contains(term)) {
                        score += 2;
                        }

                        if (description.contains(term)) {
                        score += 1;
                        }

                        return score;
                })
                .sum();
        }

    private boolean containsAny(Set<String> terms, String... expectedTerms) {
        return Arrays.stream(expectedTerms).anyMatch(terms::contains);
    }

    private boolean supportsRequestedPet(
            Product product,
            boolean catRequested,
            boolean dogRequested
    ) {
        if (!catRequested && !dogRequested) {
            return true;
        }

        // Nếu người dùng hỏi đồng thời chó và mèo thì không cần lọc.
        if (catRequested && dogRequested) {
            return true;
        }

        String petType = safe(product.getPetType())
                .trim()
                .toUpperCase(Locale.ROOT);

        String searchable = String.join(
                " ",
                safe(product.getName()),
                safe(product.getDescription()),
                safe(product.getPetType())
        ).toLowerCase(Locale.ROOT);

        if (catRequested) {
            return Set.of("CAT", "ALL", "BOTH").contains(petType)
                    || searchable.contains("mèo")
                    || searchable.contains("cat");
        }

        return Set.of("DOG", "ALL", "BOTH").contains(petType)
                || searchable.contains("chó")
                || searchable.contains("dog");
    }

    private String buildContext(
            List<ScoredChunk> chunks,
            List<Product> products
    ) {
        StringBuilder context = new StringBuilder();

        if (!chunks.isEmpty()) {
            context.append("[KIẾN THỨC THAM KHẢO]\n");

            for (ScoredChunk scored : chunks) {
                context.append("- ")
                        .append(scored.chunk().getContent())
                        .append("\n");
            }
        }

        if (!products.isEmpty()) {
            context.append("[SẢN PHẨM PHÙ HỢP]\n");

            for (Product product : products) {
                context.append("- ")
                        .append(product.getName())
                        .append(", giá ")
                        .append(product.getPrice())
                        .append(" đồng, còn ")
                        .append(product.getQuantity())
                        .append(" sản phẩm, dành cho ")
                        .append(displayPetType(product.getPetType()))
                        .append(". Mô tả: ")
                        .append(safe(product.getDescription()))
                        .append("\n");
            }
        }

        if (context.isEmpty()) {
            return "Không tìm thấy dữ liệu phù hợp.";
        }

        return context.toString();
    }

    private String displayPetType(String petType) {
        return switch (
                safe(petType).trim().toUpperCase(Locale.ROOT)
        ) {
                case "CAT" -> "mèo";
                case "DOG" -> "chó";
                case "ALL", "BOTH" -> "chó và mèo";
                default -> "thú cưng";
        };
        }

    private List<Double> parseEmbedding(KnowledgeChunk chunk) {
        try {
            return objectMapper.readValue(chunk.getEmbedding(), new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left.isEmpty() || left.size() != right.size()) {
            return -1;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.size(); index++) {
            dot += left.get(index) * right.get(index);
            leftNorm += left.get(index) * left.get(index);
            rightNorm += right.get(index) * right.get(index);
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return -1;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private void saveMessage(ChatConversation conversation, ChatRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        messageRepo.save(message);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {
    }

    private ChatIntent detectIntent(String question) {
        String normalized = normalizeText(question);
        String original = question.toLowerCase(Locale.ROOT);

        boolean checkoutRequested =
                containsAny(
                        normalized,
                        "tien hanh thanh toan",
                        "thanh toan gio hang",
                        "mo trang thanh toan",
                        "den trang thanh toan"
                )
                || (
                        normalized.contains("thanh toan")
                        && normalized.contains("gio")
                );

        if (checkoutRequested) {
        return ChatIntent.CHECKOUT_CART;
        }

        boolean addToCartRequested =
                ADD_TO_CART_PATTERN
                        .matcher(normalized)
                        .find();

        if (addToCartRequested
                && normalized.contains("thanh toan")) {
        return ChatIntent.BUY_NOW;
        }

        if (addToCartRequested) {
        return ChatIntent.ADD_TO_CART;
        }

        if (containsAny(
                normalized,
                "them vao gio",
                "bo vao gio"
        )) {
            return ChatIntent.ADD_TO_CART;
        }

        boolean explicitPurchase =
                containsAny(
                        normalized,
                        "dat hang",
                        "dat mua",
                        "muon mua",
                        "chot don",
                        "lay cho toi"
                )
                || original.startsWith("mua ")
                || original.contains(" mua ");

        if (explicitPurchase) {
            return ChatIntent.BUY_NOW;
        }

        if (containsAny(
                normalized,
                "san pham",
                "thuc an",
                "pate",
                "do choi",
                "phu kien",
                "cat ve sinh",
                "vong co",
                "quan ao",
                "gia bao nhieu",
                "con hang",
                "goi y"
        )) {
            return ChatIntent.PRODUCT_SEARCH;
        }

        if (containsAny(
                normalized,
                "cham soc",
                "dinh duong",
                "benh",
                "thu y",
                "di ung",
                "tieu chay",
                "van chuyen",
                "giao hang",
                "doi tra",
                "bao hanh",
                "chinh sach"
        )) {
            return ChatIntent.KNOWLEDGE;
        }

        return ChatIntent.SMALL_TALK;
    }

    private boolean isProductIntent(ChatIntent intent) {
        return intent == ChatIntent.PRODUCT_SEARCH
                || intent == ChatIntent.ADD_TO_CART
                || intent == ChatIntent.BUY_NOW;
    }

    private String normalizeText(String value) {
        String normalized = Normalizer.normalize(
                safe(value),
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    private boolean containsToken(String text, String... keywords) {
        String prepared = " "
                + text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N}]+", " ")
                + " ";

        return Arrays.stream(keywords)
                .anyMatch(keyword ->
                        prepared.contains(" " + keyword + " ")
                );
    }

    private record ProductMatch(
            Product product,
            int score
    ) {
    }

    private ChatAction buildAction(
            ChatIntent intent,
            List<Product> products,
            String question
    ) {
        if (intent == ChatIntent.CHECKOUT_CART) {
            return new ChatAction(
                    ChatActionType.GO_TO_CHECKOUT,
                    null,
                    null,
                    null,
                    "Mở trang thanh toán cho giỏ hàng hiện tại?"
            );
        }

        if (intent != ChatIntent.ADD_TO_CART
                && intent != ChatIntent.BUY_NOW) {
            return null;
        }

        // Không tự chọn khi có nhiều sản phẩm phù hợp.
        Product product = selectProductForAction(
                question,
                products
        );

        if (product == null) {
                return null;
        }
        int quantity = parseRequestedQuantity(question);

        // Không tạo hành động sai số lượng.
        if (quantity > product.getQuantity()) {
            return null;
        }

        ChatActionType actionType =
                intent == ChatIntent.ADD_TO_CART
                        ? ChatActionType.ADD_TO_CART
                        : ChatActionType.ADD_TO_CART_AND_CHECKOUT;

        String confirmation = actionType == ChatActionType.ADD_TO_CART
                ? "Thêm " + quantity + " × " + product.getName()
                        + " vào giỏ hàng?"
                : "Thêm " + quantity + " × " + product.getName()
                        + " vào giỏ và chuyển đến thanh toán?";

        return new ChatAction(
                actionType,
                product.getId(),
                product.getName(),
                quantity,
                confirmation
        );
    }

    private Product selectProductForAction(
        String question,
        List<Product> products
        ) {
        if (products.isEmpty()) {
                return null;
        }

        String normalizedQuestion =
                normalizeText(question);

        List<Product> nameMatches =
                products.stream()
                        .filter(product -> {
                                String productName =
                                        normalizeText(
                                                product.getName()
                                        );

                                return !productName.isBlank()
                                        && normalizedQuestion
                                                .contains(productName);
                        })
                        .toList();

        if (nameMatches.size() == 1) {
                return nameMatches.getFirst();
        }

        boolean acceptsAnyProduct = containsAny(
                normalizedQuestion,
                "bat ky",
                "tuy ban",
                "loai nao cung duoc",
                "san pham nao cung duoc"
        );

        if (acceptsAnyProduct) {
                return products.getFirst();
        }

        if (products.size() == 1) {
                return products.getFirst();
        }

        return null;
        }

    private int parseRequestedQuantity(String question) {
        String text = normalizeText(question);

        List<Pattern> patterns = List.of(
                Pattern.compile(
                        "\\b(?:mua|dat|lay|them)"
                                + "(?:\\s+cho\\s+toi)?"
                                + "\\s+(\\d{1,2})\\b"
                ),
                Pattern.compile(
                        "\\b(\\d{1,2})\\s*"
                                + "(?:goi|hop|tui|chai|san pham)\\b"
                )
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                int quantity = Integer.parseInt(matcher.group(1));
                return Math.max(1, Math.min(quantity, 99));
            }
        }

        return 1;
    }

    private String buildUserPrompt(
            ChatIntent intent,
            List<ScoredChunk> chunks,
            List<Product> products,
            String question
    ) {
        if (intent == ChatIntent.SMALL_TALK) {
            return """
                    LOẠI YÊU CẦU: trò chuyện thông thường.
                    Hãy trả lời tự nhiên và không đề xuất sản phẩm.

                    KHÁCH HÀNG:
                    """ + question;
        }

        if (intent == ChatIntent.CHECKOUT_CART) {
            return """
                    LOẠI YÊU CẦU: mở trang thanh toán.
                    Hãy mời khách xác nhận bằng nút bên dưới.
                    Không nói rằng đơn hàng đã được tạo.

                    KHÁCH HÀNG:
                    """ + question;
        }

        String context = buildContext(chunks, products);

        return """
                Dữ liệu sau chỉ dùng để tham khảo nội bộ.
                Không sao chép nguyên cấu trúc dữ liệu vào câu trả lời.

                LOẠI YÊU CẦU:
                """ + intent.name() + """

                DỮ LIỆU:
                """ + context + """

                KHÁCH HÀNG:
                """ + question;
    }
}
