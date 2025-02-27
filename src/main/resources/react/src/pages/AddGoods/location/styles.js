import styled from 'styled-components';

const WrapContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 70px;
  & > div + div {
    margin-left: 37px;
  }
`;

export { WrapContainer };
