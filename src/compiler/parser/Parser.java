package compiler.parser;

import compiler.lexer.token.Token;
import compiler.utils.TriConsumer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class Parser {
  private GrammarRule startSymbol;
  private BiConsumer<LinkedList<AbstractGrammarRule>, Token> beforeRuleApplication;
  private BiConsumer<AbstractGrammarRule, Token> onUnexpectedToken;
  private BiConsumer<AbstractGrammarRule, Token> onUnknownGrammarRule;
  private BiConsumer<AbstractGrammarRule, Token> onPredictionNotFoundError;
  private TriConsumer<AbstractGrammarRule, Token, List<AbstractGrammarRule>> onGrammarRuleApplication;

  public Parser(
    GrammarRule startSymbol,
    BiConsumer<LinkedList<AbstractGrammarRule>, Token> beforeRuleApplication,
    BiConsumer<AbstractGrammarRule, Token> onUnexpectedToken,
    BiConsumer<AbstractGrammarRule, Token> onUnknownGrammarRule,
    BiConsumer<AbstractGrammarRule, Token> onPredictionNotFoundError,
    TriConsumer<AbstractGrammarRule, Token, List<AbstractGrammarRule>> onGrammarRuleApplication
  ) {
    this.startSymbol = startSymbol;
    this.beforeRuleApplication = beforeRuleApplication;
    this.onUnexpectedToken = onUnexpectedToken;
    this.onUnknownGrammarRule = onUnknownGrammarRule;
    this.onPredictionNotFoundError = onPredictionNotFoundError;
    this.onGrammarRuleApplication = onGrammarRuleApplication;
  }

  public void parse(LinkedList<Token> tokens) throws Exception {
    final var stack = new LinkedList<AbstractGrammarRule>();
    stack.add(this.startSymbol);

    while (!tokens.isEmpty() && !stack.isEmpty()) {
      beforeRuleApplication.accept(stack, tokens.peek());
      final Token token = tokens.peek();
      final AbstractGrammarRule top = stack.pop();

      if (top instanceof Token && isEqual(token, top)) {
        tokens.pop();
        onGrammarRuleApplication.accept(top, token, Collections.emptyList());
        continue;
      }

      if (top instanceof Token) {
        onUnexpectedToken.accept(top, token);
        return;
      }


      if (!(top instanceof GrammarRule)) {
        onUnknownGrammarRule.accept(top, token);
        return;
      }

      final var rhs = ((GrammarRule) top).getRHS(token.getClass());

      if (rhs == null) {
        onPredictionNotFoundError.accept(top, token);
        return;
      }

      onGrammarRuleApplication.accept(top, token, rhs);
      for (int i = rhs.size() - 1; i >= 0; i--) {
        stack.push(rhs.get(i));
      }
    }

    if (tokens.isEmpty() && stack.isEmpty()) {
      System.out.println("Parsing successful");
    }

    if (!stack.isEmpty()) {
      throw new Exception("Failed to parse input; Expected tokens but received EOF;");
    }

    if (!tokens.isEmpty()) {
      throw new Exception(
        "Failed to parse input; Expected grammar rule but found none.\n" +
          "Remaining tokens: " + tokens.stream()
          .map(token -> token.getClass().getSimpleName())
          .collect(Collectors.toList()));
    }
  }

  public boolean isEqual(Token token, AbstractGrammarRule top) {
    return token.getClass() == top.getClass();
  }

}
